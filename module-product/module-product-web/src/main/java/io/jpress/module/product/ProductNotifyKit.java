/**
 * Copyright (c) 2016-2020, Michael Yang Fuhai (fuhai999@gmail.com).
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
package io.jpress.module.product;

import com.jfinal.template.Engine;
import io.jboot.utils.NamedThreadPools;
import io.jboot.utils.StrUtil;
import io.jpress.JPressOptions;
import io.jpress.commons.email.Email;
import io.jpress.commons.sms.SmsKit;
import io.jpress.model.User;
import io.jpress.module.product.model.Product;
import io.jpress.module.product.model.ProductComment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class ProductNotifyKit {

    private static ExecutorService fixedThreadPool = NamedThreadPools.newFixedThreadPool(3,"product-notify");

    public static void doNotifyAdministrator(Product product, ProductComment comment, User user) {
        doNotifyAdministratorByEmail(product, comment, user);
        doNotifyAdministratorBySms(product, comment);
    }

    /**
     * Send an email to the administrator and inform the website new comments
     *
     * @param product
     * @param comment
     */

    public static void doNotifyAdministratorBySms(Product product, ProductComment comment) {
        boolean enable = JPressOptions.getAsBool("product_comment_sms_notify_enable");
        if (enable) {
            fixedThreadPool.execute(() -> doSendSms());
        }
    }

    private static void doSendSms() {
        String mobile = JPressOptions.get("web_mater_mobile");
        String template = JPressOptions.get("product_comment_sms_notify_template");
        String sign = JPressOptions.get("product_comment_sms_notify_sign");

        if (!StrUtil.areNotEmpty(mobile, template, sign)) {
            return;
        }

        Set<String> mobiles = StrUtil.splitToSet(mobile, ",");
        for (String nobileNumber : mobiles) {
            SmsKit.sendSms(nobileNumber, template, sign);
        }

    }


    public static void doNotifyAdministratorByEmail(Product product, ProductComment comment, User user) {
        boolean enable = JPressOptions.getAsBool("product_comment_email_notify_enable");
        if (enable) {
            fixedThreadPool.execute(() -> doSendEmail(product, comment, user));
        }
    }


    private static void doSendEmail(Product product, ProductComment comment, User user) {

        String emailTemplate = JPressOptions.get("product_comment_email_notify_template");
        String emailTitle = JPressOptions.get("product_comment_email_notify_title");
        String webMasterEmail = JPressOptions.get("web_mater_email");

        if (!StrUtil.areNotEmpty(emailTemplate, emailTitle, webMasterEmail)) {
            return;
        }

        Map<String, Object> paras = new HashMap();
        paras.put("product", product);
        paras.put("comment", comment);
        paras.put("user", user);

        String title = Engine.use().getTemplateByString(emailTitle).renderToString(paras);
        String content = Engine.use().getTemplateByString(emailTemplate).renderToString(paras);

        Email email = Email.create();
        email.content(content);
        email.subject(title);
        email.to(webMasterEmail.split(","));
        email.send();

    }


}
