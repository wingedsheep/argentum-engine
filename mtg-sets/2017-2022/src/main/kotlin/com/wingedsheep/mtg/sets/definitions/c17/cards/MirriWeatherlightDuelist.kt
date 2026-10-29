package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mirri, Weatherlight Duelist — Commander 2017 #43 (earliest printing).
 * {1}{G}{W} · Legendary Creature — Cat Warrior · 3/2
 *
 * First strike
 * Whenever Mirri attacks, each opponent can't block with more than one creature this combat.
 * As long as Mirri is tapped, no more than one creature can attack you each combat.
 *
 *  - The attack trigger stamps each opponent with a this-combat blocker cap
 *    ([Effects.BlockerCountLimitThisCombat]), so it holds even if Mirri leaves before blocks.
 *  - The tapped clause is the per-player axis of [AttackerCountLimit] behind a
 *    `SourceIsTapped` condition. It is checked when attackers are declared, so tapping Mirri
 *    afterwards removes nobody from combat, and attacks on your planeswalkers or battles you
 *    protect aren't capped (2017-08-25 and 2023-07-28 rulings).
 */
val MirriWeatherlightDuelist = card("Mirri, Weatherlight Duelist") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Cat Warrior"
    power = 3
    toughness = 2
    oracleText = "First strike\n" +
        "Whenever Mirri attacks, each opponent can't block with more than one creature this combat.\n" +
        "As long as Mirri is tapped, no more than one creature can attack you each combat."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.BlockerCountLimitThisCombat(1)
    }

    staticAbility {
        condition = Conditions.SourceIsTapped
        ability = AttackerCountLimit(1, defendingPlayers = Player.You)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "43"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f7b6d78-4c18-4f96-9f0c-53a5c81aa6f9.jpg?1783935935"

        ruling("2023-07-28", "While Mirri is tapped, there's no restriction on how many creatures can attack planeswalkers you control or battles you protect.")
        ruling("2017-08-25", "Tapping Mirri after creatures have attacked will not remove creatures from combat.")
    }
}
