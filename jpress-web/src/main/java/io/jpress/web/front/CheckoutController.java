/**
 * Copyright (c) 2016-2019, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.web.front;

import com.jfinal.aop.Aop;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Ret;
import io.jboot.utils.StrUtil;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jpress.model.*;
import io.jpress.service.*;
import io.jpress.web.base.UcenterControllerBase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Package io.jpress.web
 */
@RequestMapping(value = "/ucenter/checkout", viewPath = "/WEB-INF/views/ucenter/")
public class CheckoutController extends UcenterControllerBase {

    @Inject
    private UserService userService;

    @Inject
    private UserCartService cartService;

    @Inject
    private UserAddressService addressService;

    @Inject
    private CouponCodeService couponCodeService;

    @Inject
    private CouponService couponService;

    @Inject
    private UserOrderService userOrderService;

    @Inject
    private UserOrderItemService userOrderItemService;


    /**
     * 购买页面
     */
    public void index() {
        List<UserCart> userCarts = new ArrayList<>();
        Long cid = getParaToLong();
        if (cid != null) {
            userCarts.add(cartService.findById(cid));
        } else {
            userCarts.addAll(cartService.findSelectedByUserId(getLoginedUser().getId()));
        }
        setAttr("userCarts", userCarts);

        UserAddress defaultAddress = addressService.findDefaultAddress(getLoginedUser().getId());
        setAttr("defaultAddress", defaultAddress);


        render("checkout.html");
    }



    /**
     * 开始购买
     */
    public void exec() {

        String[] cids = getParaValues("cid");
        if (cids == null || cids.length == 0) {
            renderFailJson();
            return;
        }

        /**
         * 创建订单项
         */
        List<UserOrderItem> userOrderItems = new ArrayList<>(cids.length);
        for (String cid : cids) {
            UserCart userCart = cartService.findById(cid);

            UserOrderItem item = new UserOrderItem();
//            item.setBuyerMsg();
            item.setBuyerId(userCart.getUserId());
            item.setBuyerNickname(getLoginedUser().getNickname());
            item.setSellerId(userCart.getSellerId());

            item.setProductId(userCart.getProductId());
            item.setProductType(userCart.getProductType());
            item.setProductTitle(userCart.getProductTitle());
            item.setProductPrice(userCart.getProductPrice());
            item.setProductCount(userCart.getProductCount());
            item.setProductThumbnail(userCart.getProductThumbnail());

            item.setTotalPrice(userCart.getProductPrice().multiply(new BigDecimal(userCart.getProductCount())));

            userOrderItems.add(item);
        }


        /**
         * 创建订单
         */
        UserOrder userOrder = getModel(UserOrder.class, "order");
        userOrder.setBuyerId(getLoginedUser().getId());
        userOrder.setBuyerMsg(getPara("buyer_msg"));
        userOrder.setBuyerNickname(getLoginedUser().getNickname());
        userOrder.setNs(PayKit.genOrderNS());
        String codeStr = getPara("coupon_code");
        if (StrUtil.isNotBlank(codeStr)) {
            CouponCode couponCode = couponCodeService.findByCode(codeStr);
            if (couponCode == null || !couponCodeService.valid(couponCode)) {
                renderJson(Ret.fail().set("message", "优惠码不可用"));
                return;
            }


            Coupon coupon = couponService.findById(couponCode.getCouponId());

            //设置优惠码
            userOrder.setCouponCode(codeStr);
            userOrder.setCouponAmount(coupon.getAmount());
        }

        //保存 order
        Object userOrderId = userOrderService.save(userOrder);
        for (UserOrderItem item : userOrderItems) {
            item.setOrderId((Long) userOrderId);
            item.setOrderNs(userOrder.getNs());
        }

        //保存 order item
        userOrderItemService.batchSave(userOrderItems);


        PaymentRecord payment = new PaymentRecord();
        payment.setProductName("用户充值");
        payment.setProductType("recharge");

        payment.setTrxNo(StrUtil.uuid());
        payment.setTrxType("product");

        payment.setPayerUserId(getLoginedUser().getId());
        payment.setPayerName(getLoginedUser().getNickname());
        payment.setPayerFee(BigDecimal.ZERO);

        payment.setOrderIp(getIPAddress());
        payment.setOrderRefererUrl(getReferer());

//        payment.setPayAmount(BigDecimal.valueOf(getParaToLong("recharge_amount")));
        payment.setPayAmount(new BigDecimal(10));
        payment.setStatus(1);

        PaymentRecordService paymentService = Aop.get(PaymentRecordService.class);

        //保存 payment
        paymentService.save(payment);

        renderJson(Ret.ok().set("paytype",getPara("paytype")).set("trxno",payment.getTrxNo()));

//        PayKit.redirect(getPara("paytype"), payment.getTrxNo());
    }


}