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
package io.jpress.module.article.controller.api;

import com.jfinal.aop.Inject;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import io.jboot.aop.annotation.DefaultValue;
import io.jboot.apidoc.ContentType;
import io.jboot.apidoc.annotation.Api;
import io.jboot.apidoc.annotation.ApiOper;
import io.jboot.apidoc.annotation.ApiPara;
import io.jboot.apidoc.annotation.ApiResp;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jboot.web.json.JsonBody;
import io.jpress.commons.Rets;
import io.jpress.module.article.model.ArticleComment;
import io.jpress.module.article.service.ArticleCommentService;
import io.jpress.web.base.ApiControllerBase;

import javax.validation.constraints.NotNull;


/**
 * @author Michael Yang （fuhai999@gmail.com）
 * @version V1.0
 * @Title: 文章评论相关 API
 */
@RequestMapping("/api/article/comment")
@Api("Article Review Related API")
public class ArticleCommentApiController extends ApiControllerBase {

    @Inject
    private ArticleCommentService commentService;


    @ApiOper("Comment on pagination query articles")
    @ApiResp(field = "page", notes = "Article comments pagination data", dataType = Page.class, genericTypes = ArticleComment.class)
    public Ret paginateByArticleId(@ApiPara("Article ID") @NotNull Long articleId
            , @ApiPara("Paging page number") @DefaultValue("1") int pageNumber
            , @ApiPara("Data volume per page") @DefaultValue("10") int pageSize) {
        return Ret.ok().set("page", commentService.paginateByArticleIdInNormal(pageNumber, pageSize, articleId));
    }


    @ApiOper("Delete comment")
    public Ret doDelete(@ApiPara("Comment ID") @NotNull Long id) {
        commentService.deleteById(id);
        return Rets.OK;
    }


    @ApiOper(value = "Create a new comment", contentType = ContentType.JSON)
    @ApiResp(field = "id", notes = "Comments ID", dataType = Long.class, mock = "123")
    public Ret doCreate(@ApiPara("Commentary json") @JsonBody ArticleComment articleComment) {
        Object id = commentService.save(articleComment);
        return Ret.ok().set("id", id);
    }


    @ApiOper(value = "Update comment information", contentType = ContentType.JSON)
    public Ret doUpdate(@ApiPara("Commentary json") @JsonBody ArticleComment articleComment) {
        commentService.update(articleComment);
        return Rets.OK;
    }

}
