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
package io.jpress.module.article.wechat;

import com.jfinal.aop.Inject;
import com.jfinal.weixin.sdk.jfinal.MsgController;
import com.jfinal.weixin.sdk.msg.in.InMsg;
import com.jfinal.weixin.sdk.msg.in.InTextMsg;
import com.jfinal.weixin.sdk.msg.out.News;
import com.jfinal.weixin.sdk.msg.out.OutNewsMsg;
import com.jfinal.weixin.sdk.msg.out.OutTextMsg;
import io.jboot.utils.StrUtil;
import io.jpress.JPressConsts;
import io.jpress.commons.utils.CommonsUtils;
import io.jpress.core.wechat.WechatAddon;
import io.jpress.core.wechat.WechatAddonConfig;
import io.jpress.module.article.model.Article;
import io.jpress.module.article.service.ArticleService;
import io.jpress.service.OptionService;

/**
 * @author Michael Yang （fuhai999@gmail.com）
 * @version V1.0
 * @Title: WeChat plug -in for articles view
 * @Description: Enter the keyword content as: article: slug
 * @Package io.jpress.module.article.wechat
 */
@WechatAddonConfig(
        id = "ip.press.article",
        title = "View article",
        description = "enter article:slug Return to the article content",
        author = "Sea brother")
public class ArticleDetailWechatAddon implements WechatAddon {

    @Inject
    ArticleService articleService;

    @Inject
    OptionService optionService;

    @Override
    public boolean onMatchingMessage(InMsg inMsg, MsgController msgController) {
        if (!(inMsg instanceof InTextMsg)) {
            return false;
        }

        InTextMsg inTextMsg = (InTextMsg) inMsg;
        String content = inTextMsg.getContent();
        return content != null && content.startsWith("article:");
    }


    @Override
    public boolean onRenderMessage(InMsg inMsg, MsgController msgController) {
        InTextMsg inTextMsg = (InTextMsg) inMsg;

        String content = inTextMsg.getContent();
        String slug = content.substring(8); // 8 =  "article:".length();

        Article article = articleService.findFirstBySlug(slug);
        if (article == null) {
            return false;
        }

        String webDomain = optionService.findByKey(JPressConsts.OPTION_WEB_DOMAIN);
        if (StrUtil.isBlank(webDomain)) {
            OutTextMsg outTextMsg = new OutTextMsg(inMsg);
            outTextMsg.setContent("Server configuration error: Website domain name configuration is empty, please go to the background first-> System-> Conventional configuration website domain name");
            msgController.render(outTextMsg);
            return true;
        }

        News news = new News();
        news.setDescription(CommonsUtils.maxLength(article.getText(), 100));
        news.setTitle(article.getTitle());
        news.setUrl(webDomain + article.getUrl());
        news.setPicUrl(webDomain + article.getThumbnail());

        OutNewsMsg outNewsMsg = new OutNewsMsg(inMsg);
        outNewsMsg.addNews(news);
        msgController.render(outNewsMsg);

        return true;
    }
}
