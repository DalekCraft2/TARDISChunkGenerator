/*
 * Copyright (C) 2020 eccentric_nz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (location your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.eccentric_nz.tardischunkgenerator.disguise;

import net.minecraft.world.entity.npc.VillagerProfession;
import org.bukkit.entity.Villager;

public enum PROFESSION {

    NONE(VillagerProfession.a), // NONE
    ARMORER(VillagerProfession.b), // ARMORER
    BUTCHER(VillagerProfession.c), // BUTCHER
    CARTOGRAPHER(VillagerProfession.d), // CARTOGRAPHER
    CLERIC(VillagerProfession.e), // CLERIC
    FARMER(VillagerProfession.f), // FARMER
    FISHERMAN(VillagerProfession.g), // FISHERMAN
    FLETCHER(VillagerProfession.h), // FLETCHER
    LEATHERWORKER(VillagerProfession.i), // LEATHERWORKER
    LIBRARIAN(VillagerProfession.j), // LIBRARIAN
    MASON(VillagerProfession.k), // MASON
    NITWIT(VillagerProfession.l), // NITWIT
    SHEPHERD(VillagerProfession.m), // SHEPHERD
    TOOLSMITH(VillagerProfession.n), // TOOLSMITH
    WEAPONSMITH(VillagerProfession.o); // WEAPONSMITH

    private final VillagerProfession nms;

    PROFESSION(VillagerProfession nmsProfession) {
        nms = nmsProfession;
    }

    public static PROFESSION getFromVillagerProfession(Villager.Profession profession) {
        if (profession == null) {
            return PROFESSION.NONE;
        }
        return PROFESSION.valueOf(profession.toString());
    }

    public VillagerProfession getNmsProfession() {
        return nms;
    }
}
