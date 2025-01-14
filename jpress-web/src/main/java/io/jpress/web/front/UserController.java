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
package io.jpress.web.front;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.kit.HashKit;
import com.jfinal.kit.LogKit;
import com.jfinal.kit.Ret;
import com.jfinal.template.Engine;
import io.jboot.utils.CacheUtil;
import io.jboot.utils.CookieUtil;
import io.jboot.utils.RequestUtil;
import io.jboot.utils.StrUtil;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jboot.web.json.JsonBody;
import io.jboot.web.validate.EmptyValidate;
import io.jboot.web.validate.Form;
import io.jboot.web.validate.Regex;
import io.jpress.JPressConsts;
import io.jpress.JPressOptions;
import io.jpress.commons.authcode.AuthCode;
import io.jpress.commons.authcode.AuthCodeKit;
import io.jpress.commons.email.Email;
import io.jpress.commons.email.EmailKit;
import io.jpress.commons.email.SimpleEmailSender;
import io.jpress.commons.sms.SmsKit;
import io.jpress.commons.utils.SessionUtils;
import io.jpress.model.User;
import io.jpress.service.UserService;
import io.jpress.web.base.TemplateControllerBase;
import io.jpress.web.commons.email.EmailSender;

import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Michael Yang （fuhai999@gmail.com）
 * @version V1.0
 * @Package io.jpress.web
 */
@RequestMapping("/user")
public class UserController extends TemplateControllerBase {

    private static final String default_user_login_template = "/WEB-INF/views/ucenter/user_login.html";
    private static final String default_user_register_template = "/WEB-INF/views/ucenter/user_register.html";
    private static final String default_user_phone_register_template = "/WEB-INF/views/ucenter/user_phone_register.html";
    private static final String default_user_register_activate = "/WEB-INF/views/ucenter/user_activate.html";
    private static final String default_user_register_emailactivate = "/WEB-INF/views/ucenter/user_emailactivate.html";
    private static final String default_user_retrieve_password = "/WEB-INF/views/ucenter/user_retrieve_password.html";
    private static final String default_send_link_to_user = "/WEB-INF/views/ucenter/send_link_to_user.html";
    private static final String default_user_reset_password = "/WEB-INF/views/ucenter/user_reset_password.html";


    @Inject
    private UserService userService;

    @Inject
    private CaptchaService captchaService;




    /**
     * 用户信息页面
     */
    public void index() {

        //不支持渲染用户详情
        if (hasTemplate("user_detail.html") == false) {
            renderError(404);
            return;
        }

        Long id = getParaToLong();
        if (id == null) {
            renderError(404);
            return;
        }

        User user = userService.findById(id);
        if (user == null) {
            renderError(404);
            return;
        }

        setAttr("user", user.keepSafe());
        render("user_detail.html");
    }

    /**
     * 用户登录页面
     */
    public void login() {
        render("user_login.html", default_user_login_template);
    }

    @Clear
    @EmptyValidate({
            @Form(name = "user", message = "The account cannot be empty"),
            @Form(name = "pwd", message = "password can not be blank")
    })
    public void doLogin(String user, String pwd) {

        pwd = getOriginalPara("pwd");

        if (StrUtil.isBlank(user) || StrUtil.isBlank(pwd)) {
            LogKit.error("Your current IDEA or Eclipse configuration has problems, please refer to the document: http://www.jfinal.com/doc/3-3 Configuration");
            return;
        }

        if (JPressOptions.getAsBool("login_captcha_enable",true)) {
            String captcha = get("captcha");
            if (StrUtil.isBlank(captcha)){
                renderJson(Ret.fail().set("message", "verification code must be filled").set("errorCode", 7));
                return;
            }
            if (!validateCaptcha("captcha")) {
                renderJson(Ret.fail().set("message", "Incorrect verification code").set("errorCode", 7));
                return;
            }
        }

        User loginUser = userService.findByUsernameOrEmail(user);
        if (loginUser == null) {
            renderJson(Ret.fail("message", "The username is incorrect."));
            return;
        }

        Ret ret = userService.doValidateUserPwd(loginUser, pwd);

        if (ret.isOk()) {
            SessionUtils.record(loginUser.getId());
            CookieUtil.put(this, JPressConsts.COOKIE_UID, loginUser.getId());
        }

        String gotoUrl = JPressOptions.get("login_goto_url", "/ucenter");
        ret.set("gotoUrl", gotoUrl);

        renderJson(ret);
    }

    /**
     * 用户注册页面
     */
    public void register() {
        render("user_register.html", default_user_register_template);
    }

    /**
     * 手机号注册页面
     */
    public void phoneRegister() {
        render("user_phone_register.html", default_user_phone_register_template);
    }


    /**
     * 用户激活页面
     */
    public void activate() {
        String id = getPara("id");
        if (StrUtil.isBlank(id)) {
            renderError(404);
            return;
        }

        AuthCode authCode = AuthCodeKit.get(id);
        if (authCode == null) {
            setAttr("code", 1);
            setAttr("message", "The link has been invalid, you can try to send the activation email again");
            render("user_activate.html", default_user_register_activate);
            return;
        }

        User user = userService.findById(authCode.getUserId());
        if (user == null) {
            setAttr("code", 2);
            setAttr("message", "Users do not exist or have been deleted");
            render("user_activate.html", default_user_register_activate);
            return;
        }

        user.setStatus(User.STATUS_OK);
        userService.update(user);

        setAttr("code", 0);
        setAttr("user", user);
        render("user_activate.html", default_user_register_activate);
    }


    /**
     * 邮件激活
     */
    public void emailactivate() {
        String id = getPara("id");
        if (StrUtil.isBlank(id)) {
            renderError(404);
            return;
        }

        AuthCode authCode = AuthCodeKit.get(id);
        if (authCode == null) {
            setAttr("code", 1);
            setAttr("message", "The link has failed, you can try to send activation emails again at the user center again");
            render("user_emailactivate.html", default_user_register_emailactivate);
            return;
        }

        User user = userService.findById(authCode.getUserId());
        if (user == null) {
            setAttr("code", 2);
            setAttr("message", "Users do not exist or have been deleted");
            render("user_emailactivate.html", default_user_register_emailactivate);
            return;
        }

        user.setEmailStatus(User.STATUS_OK);
        userService.update(user);

        setAttr("code", 0);
        setAttr("user", user);
        render("user_emailactivate.html", default_user_register_emailactivate);
    }


    public void doRegister() {

        boolean regEnable = JPressOptions.isTrueOrEmpty("reg_enable");
        if (!regEnable) {
            renderJson(Ret.fail().set("message", "The registration function has been closed").set("errorCode", 12));
            return;
        }

        String username = getPara("username");
        String email = getPara("email");
        String pwd = getPara("pwd");
        String confirmPwd = getPara("confirmPwd");

        if (StrUtil.isBlank(username)) {
            renderJson(Ret.fail().set("message", "Username can not be empty").set("errorCode", 1));
            return;
        }

        if (StrUtil.isBlank(email)) {
            renderJson(Ret.fail().set("message", "E-mail can not be empty").set("errorCode", 2));
            return;
        } else {
            email = email.toLowerCase();
        }

        if (StrUtil.isBlank(pwd)) {
            renderJson(Ret.fail().set("message", "password can not be blank").set("errorCode", 3));
            return;
        }

        if (StrUtil.isBlank(confirmPwd)) {
            renderJson(Ret.fail().set("message", "confirm password can not be blank").set("errorCode", 4));
            return;
        }

        if (!pwd.equals(confirmPwd)) {
            renderJson(Ret.fail().set("message", "Two input passwords are inconsistent").set("errorCode", 5));
            return;
        }

        if (StrUtil.isBlank(getPara("captcha"))) {
            renderJson(Ret.fail().set("message", "verification code must be filled").set("errorCode", 6));
            return;
        }

        if (!EmailKit.validateCode(email,getPara("captcha"))) {
            renderJson(Ret.fail().set("message", "Incorrect verification code").set("errorCode", 7));
            return;
        }


        User user = userService.findFirstByUsername(username);
        if (user != null) {
            renderJson(Ret.fail().set("message", "This user name already exists").set("errorCode", 10));
            return;
        }

        user = userService.findFirstByEmail(email);
        if (user != null) {
            renderJson(Ret.fail().set("message", "The mailbox already exists").set("errorCode", 11));
            return;
        }

        String salt = HashKit.generateSaltForSha256();
        String hashedPass = HashKit.sha256(salt + pwd);

        user = new User();
        user.setUsername(username);
        user.setNickname(username);
        user.setRealname(username);
        user.setEmail(email.toLowerCase());
        user.setSalt(salt);
        user.setPassword(hashedPass);
        user.setCreated(new Date());

        user.setCreateSource(User.SOURCE_WEB_REGISTER);
        user.setAnonym(CookieUtil.get(this, JPressConsts.COOKIE_ANONYM));

        // 是否启用邮件验证
        boolean emailValidate = JPressOptions.getAsBool("reg_email_validate_enable");
        if (emailValidate) {
            user.setStatus(User.STATUS_REG);
        } else {
            user.setStatus(User.STATUS_OK);
        }

        //强制用户状态为未激活
        boolean isNotActivate = JPressOptions.getAsBool("reg_users_is_not_activate");
        if (isNotActivate) {
            user.setStatus(User.STATUS_REG);
        }

        Object userId = userService.save(user);

        if (userId != null && emailValidate) {
            EmailSender.sendForUserActivate(user);
        }

        renderJson(user != null ? OK : FAIL);
    }

    /**
     * 手机号注册
     */
    public void doPhoneRegister() {

        boolean regEnable = JPressOptions.isTrueOrEmpty("reg_enable");
        if (!regEnable) {
            renderJson(Ret.fail().set("message", "The registration function has been closed").set("errorCode", 12));
            return;
        }

        String username = getPara("username");
        String phone = getPara("phone");
        String pwd = getPara("pwd");
        String confirmPwd = getPara("confirmPwd");

        if (StrUtil.isBlank(username)) {
            renderJson(Ret.fail().set("message", "Username can not be empty").set("errorCode", 1));
            return;
        }

        if (StrUtil.isBlank(phone)) {
            renderJson(Ret.fail().set("message", "The mobile phone number cannot be empty").set("errorCode", 2));
            return;
        }

        if (StrUtil.isBlank(pwd)) {
            renderJson(Ret.fail().set("message", "password can not be blank").set("errorCode", 3));
            return;
        }

        if (StrUtil.isBlank(confirmPwd)) {
            renderJson(Ret.fail().set("message", "confirm password can not be blank").set("errorCode", 4));
            return;
        }

        if (!pwd.equals(confirmPwd)) {
            renderJson(Ret.fail().set("message", "Two input passwords are inconsistent").set("errorCode", 5));
            return;
        }

        if (StrUtil.isBlank(getPara("captcha"))) {
            renderJson(Ret.fail().set("message", "verification code must be filled").set("errorCode", 6));
            return;
        }

        if (!SmsKit.validateCode(phone,getPara("captcha"))) {
            renderJson(Ret.fail().set("message", "Incorrect verification code").set("errorCode", 7));
            return;
        }

        User user = userService.findFirstByUsername(username);
        if (user != null) {
            renderJson(Ret.fail().set("message", "This user name already exists").set("errorCode", 10));
            return;
        }

        user = userService.findFirstByMobile(phone);
        if (user != null) {
            renderJson(Ret.fail().set("message", "The phone number has been registered").set("errorCode", 11));
            return;
        }

        String salt = HashKit.generateSaltForSha256();
        String hashedPass = HashKit.sha256(salt + pwd);

        user = new User();
        user.setUsername(username);
        user.setNickname(username);
        user.setRealname(username);
        user.setMobile(phone);
        user.setSalt(salt);
        user.setPassword(hashedPass);
        user.setCreated(new Date());

        user.setMobileStatus("ok" );

        user.setCreateSource(User.SOURCE_WEB_REGISTER);
        user.setAnonym(CookieUtil.get(this, JPressConsts.COOKIE_ANONYM));

        //是否启用短信验证
        boolean smsValidate = JPressOptions.getAsBool("reg_sms_validate_enable");
        if (smsValidate) {
            user.setStatus(User.STATUS_REG);
        }else {
            user.setStatus(User.STATUS_OK);
        }

        //强制用户状态为未激活
        boolean isNotActivate = JPressOptions.getAsBool("reg_users_is_not_activate");
        if (isNotActivate) {
            user.setStatus(User.STATUS_REG);
        }

        userService.save(user);

        renderJson(user != null ? OK : FAIL);
    }


    /**
     * 发送短信验证码
     * @param mobile
     * @param captchaVO
     */
    public void doSendLoginCode(@Pattern(regexp = Regex.MOBILE) @JsonBody("mobile") String mobile, @JsonBody CaptchaVO captchaVO) {
        ResponseModel validResult = captchaService.verification(captchaVO);
        if (validResult != null && validResult.isSuccess()) {
            String code = SmsKit.generateCode();
            String template = JPressOptions.get("reg_sms_validate_template");
            String sign = JPressOptions.get("reg_sms_validate_sign");

            boolean sendOk = SmsKit.sendCode(mobile, code, template, sign);

            if (sendOk) {
                renderJson(Ret.ok().set("message", "Successful SMS sending, please check your mobile phone"));
            } else {
                renderJson(Ret.fail().set("message", "SMS failed, please contact the administrator"));
            }
        } else {
            renderFailJson("Verification error");
        }
    }



    /**
     * 找回密码
     */
    public void retrievePwd() {
        render("user_retrieve_password.html", default_user_retrieve_password);
    }

    /**
     * 发送重置密码的链接给用户
     */
    public void sendLink() {
        render("send_link_to_user.html", default_send_link_to_user);
    }

    /**
     * 重置密码
     */
    public void resetPwd() {
        String token = getPara("token");
        setAttr("token",token);

        Boolean isEmail = getParaToBoolean("isEmail");
        setAttr("isEmail",isEmail);

        render("user_reset_password.html", default_user_reset_password);
    }


    /**
     * 给邮箱账号发送重置密码的链接地址
     * @param emailAddr
     * @param captchaVO
     */
    public  void doSendResetPwdLinkToEmail(@Pattern(regexp = Regex.EMAIL) @JsonBody("email")  String emailAddr,@JsonBody  CaptchaVO captchaVO) {
        ResponseModel validResult = captchaService.verification(captchaVO);
        if (validResult != null && validResult.isSuccess()) {
            if (!StrUtil.isEmail(emailAddr)) {
                renderFailJson("The mailbox address you entered is wrong.");
                return;
            }

            User user = userService.findFirstByEmail(emailAddr);
            if(user == null){
                renderJson(Ret.fail().set("message", "The mailbox is not registered or has been deleted").set("errorCode", 11));
                return;
            }

            SimpleEmailSender ses = new SimpleEmailSender();
            if (!ses.isEnable()) {
                renderFailJson("You can't send it without opening the mail function.");
                return;
            }

            if (!ses.isConfigOk()) {
                renderFailJson("Unconfigured is correct, SMTP or user name or password is empty.");
                return;
            }

            //生成唯一不重复的字符串
            String token = UUID.randomUUID().toString();
            //token和邮箱绑定
            CacheUtil.put("email_token",token,emailAddr,60 * 60);
            CacheUtil.put("email_token",emailAddr,token,60 * 60);

            String webDomain = JPressOptions.get(JPressConsts.OPTION_WEB_DOMAIN);
            if (StrUtil.isBlank(webDomain)){
                webDomain = RequestUtil.getBaseUrl();
            }
            String url = webDomain + "/user/resetPwd?token=" + token+"&isEmail=true";

            String webName = JPressOptions.get(JPressConsts.ATTR_WEB_NAME);
            if (StrUtil.isNotBlank(webName)) {
                webName = webName+":";
            }else{
                webName ="";
            }

            String title = webName + JPressOptions.get("reg_email_reset_pwd_title");
            String template = JPressOptions.get("reg_email_reset_pwd_template");

            String contentLink = null;
            if(StrUtil.isNotBlank(template)){
                Map<String, Object> paras = new HashMap();
                paras.put("user", user);
                paras.put("url", url);
                contentLink = Engine.use().getTemplateByString(template).renderToString(paras);
            }else{
                contentLink = "Reset password address: <a href=\"" + url + "\">" + url + "</a>";
            }

            //获取关于邮箱的配置内容
            Email email = Email.create();
            email.subject(title);
            email.content(contentLink);


            email.to(emailAddr);
            String emailNumber = CacheUtil.get("email_token", token);

            //发送邮箱重置密码链接
            EmailKit.sendResetPwdLinkToEmail(email);
            renderJson(Ret.ok().set("message", "The mailbox reset password link is successful, please check the mobile phone").set("email",emailNumber).set("isEmail",true));
        } else {
            renderFailJson("Verification error");
        }
    }


    /**
     * 重置密码 给手机账号发送验证码
     * @param mobile
     * @param captchaVO
     */
    public void doResetPwdSendCodeToMobile(@Pattern(regexp = Regex.MOBILE) @JsonBody("mobile") String mobile, @JsonBody CaptchaVO captchaVO) {
        ResponseModel validResult = captchaService.verification(captchaVO);
        if (validResult != null && validResult.isSuccess()) {
            //生成唯一不重复的字符串
            String token = UUID.randomUUID().toString();
            //token和电话号码绑定
            CacheUtil.put("mobile_token", token, mobile, 60 * 60);
            CacheUtil.put("mobile_token", mobile, token, 60 * 60);

            String code = SmsKit.generateCode();
            String template = JPressOptions.get("reg_sms_reset_pwd_template");
            String sign = JPressOptions.get("reg_sms_validate_sign");

            boolean sendOk = SmsKit.sendCode(mobile, code, template, sign);

            if (sendOk) {
                renderJson(Ret.ok().set("message", "The SMS verification code is successfully sent, please check your mobile phone!").set("mobile", mobile).set("isEmail", false));
            } else {
                renderJson(Ret.fail().set("message", "SMS failed, please contact the administrator"));
            }

        } else {
            renderFailJson("Verification error");
        }
    }

    /**
     * 重置用户密码
     */
    public void doResetPassword() {

        String token = getPara("token") == null ? "" : getPara("token");
        //根据cacheName和token从缓存中获取数据value
        String email = CacheUtil.get("email_token", token) == null ? "": CacheUtil.get("email_token", token);
        String mobile = CacheUtil.get("mobile_token", token) == null ? "": CacheUtil.get("mobile_token", token);
        //根据缓存名称获取存入的token
        String emailToken = CacheUtil.get("email_token", email);
        String mobileToken = CacheUtil.get("mobile_token", mobile);

        Boolean isEmail = getParaToBoolean("type");

        String pwd = getPara("pwd");
        String confirmPwd = getPara("confirmPwd");

        if (StrUtil.isBlank(token)) {
            renderJson(Ret.fail().set("message", "Token cannot be empty, please send it again").set("errorCode", 2));
            return;
        }
        if(isEmail && StrUtil.isNotBlank(emailToken) && !token.equals(emailToken)){
            renderJson(Ret.fail().set("message", "Token is invalid, please send it again!").set("errorCode", 5));
            return;
        }

        if (isEmail && StrUtil.isBlank(email)) {
            renderJson(Ret.fail().set("message", "The mailbox does not exist or has been deleted").set("errorCode", 2));
            return;
        } else {
            email = email.toLowerCase();
        }

        if(!isEmail&& StrUtil.isNotBlank(mobileToken)  && !mobileToken.equals(token)){
            renderJson(Ret.fail().set("message", "Token is invalid, please send it again!").set("errorCode", 5));
            return;
        }

        if (!isEmail && StrUtil.isBlank(mobile)) {
            renderJson(Ret.fail().set("message", "The mobile phone number does not exist or has been deleted").set("errorCode", 2));
            return;
        }

        if (StrUtil.isBlank(pwd)) {
            renderJson(Ret.fail().set("message", "password can not be blank").set("errorCode", 3));
            return;
        }

        if (StrUtil.isBlank(confirmPwd)) {
            renderJson(Ret.fail().set("message", "confirm password can not be blank").set("errorCode", 4));
            return;
        }

        if (!pwd.equals(confirmPwd)) {
            renderJson(Ret.fail().set("message", "Two input passwords are inconsistent").set("errorCode", 5));
            return;
        }

        if(isEmail){
            User user = userService.findFirstByEmail(email);
            if (user != null) {

                String salt = HashKit.generateSaltForSha256();
                String hashedPass = HashKit.sha256(salt + pwd);

                user.setSalt(salt);
                user.setPassword(hashedPass);
                userService.update(user);
            }else{
                renderJson(Ret.fail().set("message", "The mailbox does not exist or has been deleted").set("errorCode", 11));
                return;
            }
            renderJson(user != null ? OK.set("message","Reset password success") : FAIL.set("message","Reset password failure"));
        }else{
            User user = userService.findFirstByMobile(mobile);
            if (user != null) {

                String salt = HashKit.generateSaltForSha256();
                String hashedPass = HashKit.sha256(salt + pwd);

                user.setSalt(salt);
                user.setPassword(hashedPass);
                userService.update(user);
            }else{
                renderJson(Ret.fail().set("message", "The phone number does not exist or has been deleted").set("errorCode", 11));
                return;
            }
            renderJson(user != null ? OK.set("message","Reset password success") : FAIL.set("message","Reset password failure"));
        }
    }


    /**
     * 通过手机号找回密码
     * 验证输入的验证码是否正确
     * 验证手机号是否注册
     */
    public void validateCodeToResetPwd(){

        String phone = getPara("mobile");
        Object token = CacheUtil.get("mobile_token", phone);

        if (StrUtil.isBlank(getPara("captcha"))) {
            renderJson(Ret.fail().set("message", "verification code must be filled").set("errorCode", 6));
            return;
        }

        if (!SmsKit.validateCode(phone,getPara("captcha"))) {
            renderJson(Ret.fail().set("message", "Incorrect verification code").set("errorCode", 7));
            return;
        }


        User user = userService.findFirstByMobile(phone);

        if (user == null) {
            renderJson(Ret.fail().set("message", "The mobile phone number is not registered or has been deleted, please re -enter!").set("errorCode", 11));
            return;
        }else{

            String webDomain = JPressOptions.get(JPressConsts.OPTION_WEB_DOMAIN);
            if (StrUtil.isBlank(webDomain)){
                webDomain = RequestUtil.getBaseUrl();
            }
            String url = webDomain + "/user/resetPwd?token=" + token+"&isEmail=false";
            renderJson(Ret.ok().set("message","The verification is correct, you can reset your password!").set("url",url));
        }


    }



}




