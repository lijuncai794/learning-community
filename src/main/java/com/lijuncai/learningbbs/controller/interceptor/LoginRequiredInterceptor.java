package com.lijuncai.learningbbs.controller.interceptor;

import com.lijuncai.learningbbs.annotaion.LoginRequired;
import com.lijuncai.learningbbs.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @description:
 * @author: lijuncai
 **/
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder hostHolder;

    /**
     * 在Controller处理之前判断该方法是否需要登录
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  被拦截的对象
     * @return 处理状态
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断被拦截的对象是不是一个方法
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            //判断方法上是否有@LoginRequired注解，且用户当前未登录
            if (loginRequired != null && hostHolder.getUser() == null) {
                //重定向到登录页面
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        return true;
    }
}