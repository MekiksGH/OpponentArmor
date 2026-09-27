// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Mekiks

package com.mekiks.opponentarmor;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Puts the settings screen behind the "Configure" button in Mod Menu. */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OpponentArmorScreen::new;
    }
}
