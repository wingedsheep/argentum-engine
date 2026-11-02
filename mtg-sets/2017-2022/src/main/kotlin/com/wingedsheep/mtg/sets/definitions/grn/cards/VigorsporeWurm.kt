package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vigorspore Wurm
 * {5}{G}
 * Creature — Wurm
 * 6/4
 * Undergrowth — When this creature enters, target creature gains vigilance and gets +X/+X until
 * end of turn, where X is the number of creature cards in your graveyard.
 * This creature can't be blocked by more than one creature.
 *
 * X is locked in as the trigger resolves ([Effects.ModifyStats] with a dynamic amount).
 */
val VigorsporeWurm = card("Vigorspore Wurm") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    oracleText = "Undergrowth — When this creature enters, target creature gains vigilance and gets +X/+X " +
        "until end of turn, where X is the number of creature cards in your graveyard.\n" +
        "This creature can't be blocked by more than one creature."
    power = 6
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        val x = DynamicAmounts.creatureCardsInYourGraveyard()
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, t) then Effects.ModifyStats(x, x, t)
    }

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b62625de-3a2f-43d6-8cf7-ea38f038f3b6.jpg"
        ruling("2018-10-05", "The value of X is determined only as the undergrowth ability resolves. If the number of creature cards in your graveyard changes later in the turn, the target creature is unaffected.")
        ruling("2018-10-05", "If there are no creature cards in your graveyard, the target creature just gains vigilance until end of turn.")
        ruling("2018-10-05", "If Vigorspore Wurm gains menace, it can't be blocked at all.")
        ruling("2018-10-05", "Because tokens aren't cards, they never count for undergrowth abilities.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
    }
}
