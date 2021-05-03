package com.lijuncai.learningbbs.controller;

import com.google.code.kaptcha.Producer;
import com.lijuncai.learningbbs.entity.User;
import com.lijuncai.learningbbs.service.UserService;
import com.lijuncai.learningbbs.util.LearningBbsConstant;
import jdk.nashorn.internal.runtime.Context;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @description: 登录、注册控制类
 * @author: lijuncai
 **/
@Controller
public class LoginController implements LearningBbsConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    UserService userService;
    @Autowired
    private Producer kaptchaProducer;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 访问注册页面
     *
     * @return 注册页面模板地址
     */
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    /**
     * 访问登录页面
     *
     * @return 注册页面模板地址
     */
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    /**
     * 生成登录所需要的验证码
     *
     * @param response 响应对象
     * @param session  会话对象
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        //kaptcha配置好之后，在本类进行Producer的依赖注入
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        session.setAttribute("kaptcha", text);

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    /**
     * 用户注册:提交信息进行注册
     *
     * @param model model对象，SpringMVC自动注入
     * @param user  user对象，页面传入的参数名与user属性名相匹配则自动注入
     * @return
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> registerStatusMap = userService.register(user);
        if (registerStatusMap == null || registerStatusMap.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", registerStatusMap.get("usernameMsg"));
            model.addAttribute("passwordMsg", registerStatusMap.get("passwordMsg"));
            model.addAttribute("emailMsg", registerStatusMap.get("emailMsg"));
            return "/site/register";
        }
    }

    /**
     * 用户激活
     *
     * @param model
     * @return
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId,
                             @PathVariable("code") String code) {
        int activationState = userService.activation(userId, code);
        if (activationState == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号可以正常登录使用了!");
            model.addAttribute("target", "/login");
        } else if (activationState == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活链接无效,激活失败,请联系管理员!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    /**
     * 用户登录:提交信息进行登录
     *
     * @param username   用户名
     * @param password   密码
     * @param captcha    验证码
     * @param rememberme 是否勾选记住我选项
     * @param model      model对象
     * @param session    会话对象
     * @param response   响应对象
     * @return 登录流程的状态
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String captcha, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response) {
        //检查验证码
        String kaptcha = (String) session.getAttribute("kaptcha");
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(captcha) || !kaptcha.equalsIgnoreCase(captcha)) {
            model.addAttribute("captchaMsg", "验证码不正确!");
            return "/site/login";
        }

        //检查账号和密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    /**
     * 退出登录状态
     *
     * @param ticket 登录凭证
     * @return 返回到首页模板
     */
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "redirect:/login";
    }

}
