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
package io.jpress.commons.layer;


import java.util.List;

/**
 * @author Michael Yang （fuhai999@gmail.com）
 * @version V1.0
 * @Title: 用于可以排序和父子分类等模型
 * @Package io.jpress.commons.layer
 */
public interface SortModel<M extends SortModel> {

    public boolean isTop();

    public Long getId();

    public Long getParentId();

    public void setParent(M parent);

    public M getParent();

    public void setChilds(List<M> childs);

    public void addChild(M child);

    public List<M> getChilds();

    public void setLayerNumber(int layerNumber);

    public int getLayerNumber();


}
