package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Armguard Familiar — Kamigawa: Neon Dynasty #46 (canonical printing)
 * {1}{U} · Artifact Creature — Equipment Beast · 2/1
 *
 * Ward {2}
 * Equipped creature gets +2/+1 and has ward {2}.
 * Reconfigure {4}
 *
 * The Familiar's own ward stays with it while attached (ward works on any permanent).
 */
val ArmguardFamiliar = card("Armguard Familiar") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Equipment Beast"
    power = 2
    toughness = 1
    oracleText = "Ward {2} (Whenever this permanent becomes the target of a spell or ability an opponent " +
        "controls, counter it unless that player pays {2}.)\n" +
        "Equipped creature gets +2/+1 and has ward {2}.\n" +
        "Reconfigure {4} ({4}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    staticAbility { ability = ModifyStats(2, 1) }

    staticAbility { ability = GrantWard(WardCost.Mana("{2}")) }

    reconfigure("{4}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7497f147-146d-4a76-b670-bd84e07352b3.jpg?1783923907"
    }
}
