package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pawn of Ulamog
 * {1}{B}{B}
 * Creature — Vampire Shaman
 * 2/2
 * Whenever this creature or another nontoken creature you control dies, you may create a 0/1
 * colorless Eldrazi Spawn creature token. It has "Sacrifice this token: Add {C}."
 *
 * Two dies triggers, one per half of the text: "this creature" is a self trigger with no token
 * restriction (a token copy of Pawn still triggers on its own death), and "another nontoken
 * creature you control" excludes the source. Leaves-the-battlefield triggers look back, so Pawn
 * sees its own death and creatures dying alongside it.
 */
val PawnOfUlamog = card("Pawn of Ulamog") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or another nontoken creature you control dies, you may create a " +
        "0/1 colorless Eldrazi Spawn creature token. It has \"Sacrifice this token: Add {C}.\""

    triggeredAbility {
        trigger = Triggers.self.dies()
        optional = true
        effect = Effects.CreateEldraziSpawn()
        description = "Whenever this creature or another nontoken creature you control dies, you may " +
            "create a 0/1 colorless Eldrazi Spawn creature token. It has \"Sacrifice this token: Add {C}.\""
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        optional = true
        effect = Effects.CreateEldraziSpawn()
        description = "Whenever this creature or another nontoken creature you control dies, you may " +
            "create a 0/1 colorless Eldrazi Spawn creature token. It has \"Sacrifice this token: Add {C}.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85f4d77d-e0de-48fe-899b-5718cee779e2.jpg?1783941982"
        ruling("2010-06-15", "If you control more than one Pawn of Ulamog and a nontoken creature you control is put into a graveyard, each of those Pawns' abilities will trigger. You may put up to that many Eldrazi Spawn tokens onto the battlefield.")
        ruling("2010-06-15", "If multiple nontoken creatures you control (possibly including Pawn of Ulamog itself) are put into their owners' graveyards at the same time, Pawn of Ulamog's triggered ability will trigger that many times.")
    }
}
