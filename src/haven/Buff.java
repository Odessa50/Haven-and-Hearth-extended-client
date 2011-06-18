/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.resources.layers.Tooltip;

import java.awt.*;

public class Buff {
    public static final Text.Foundry nfnd = new Text.Foundry("SansSerif", 10);
    final int id;
    Indir<Resource> res;
    String tt = null;
    public int ameter = -1; // полоска под бафом от 0 до 100
    int nmeter = -1; // некий счетчик хз???
    int cmeter = -1; // прогресс бафа. круглешок от 0 до 100
    int cticks = -1; // количество тиков для полного круглешка определяет время за которое спадет круглешок. некий кеш на клиенте
    // чтобы постоянно не опрашивать сервер о прогрессе круглешка
    long gettime;
    Tex ntext = null;
    boolean major = false;

    public Buff(int id, Indir<Resource> res) {
        this.id = id;
        this.res = res;
    }

    Tex nmeter() {
        if (ntext == null)
            ntext = new TexI(Utils.outline2(nfnd.render(Integer.toString(nmeter), Color.WHITE).img, Color.BLACK));
        return (ntext);
    }

    // arksu 19.12.2010 lets rock it!
    // получить тул тип в виде текста
    public String getName() {
        Tooltip tt;
        if ((res.get() != null) && ((tt = res.get().layer(Resource.tooltip)) != null)) {
            return tt.t;
        } else {
            return "";
        }
    }

    // получить время до конца бафа от 0 до 100
    public int getTimeLeft() {
        if (cmeter >= 0) {
            long now = System.currentTimeMillis();
            double m = cmeter / 100.0;
            if (cticks >= 0) {
                double ot = cticks * 0.06;
                double pt = ((double) (now - gettime)) / 1000.0;
                m *= (ot - pt) / ot;
            }
            return (int) Math.round(m * 100);
        }
        return 0;
    }
}
