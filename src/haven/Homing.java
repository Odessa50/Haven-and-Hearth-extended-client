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

public class Homing extends Moving {
    final int tgt;
    Coord tc;
    int v;
    double dist;

    public Homing(Gob gob, int tgt, Coord tc, int v) {
        super(gob);
        this.tgt = tgt;
        this.tc = tc;
        this.v = v;
    }

    public Coord getc() {
        Coord tc = this.tc;
        Gob tgt = gob.glob.oc.getgob(this.tgt);
        if (tgt != null)
            tc = tgt.rc;
        Coord d = tc.sub(gob.rc);
        double e = gob.rc.dist(tc);
        return (gob.rc.add((int) ((d.x / e) * dist), (int) ((d.y / e) * dist)));
    }

    public void move(Coord c) {
        dist = 0;
    }

    public void ctick(int dt) {
        double da = ((double) dt / 1000) / 0.06;
        dist += (da * 0.9) * ((double) v / 100);
    }
}
