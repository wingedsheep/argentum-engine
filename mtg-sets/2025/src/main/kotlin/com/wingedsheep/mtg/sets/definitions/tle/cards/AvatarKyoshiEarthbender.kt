package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Avatar Kyoshi, Earthbender
 * {5}{G}{G}{G}
 * Legendary Creature — Human Avatar
 * 6/6
 * During your turn, Avatar Kyoshi has hexproof.
 * At the beginning of combat on your turn, earthbend 8, then untap that land.
 *
 * Hexproof on your turn is Pouncing Lynx's shape: a [ConditionalStaticAbility] over
 * `GrantKeyword(HEXPROOF, Filters.Self)` gated by [Conditions.IsYourTurn]. The combat trigger is
 * [Effects.Earthbend] on a targeted land you control followed by [Effects.Untap] on the same
 * target; if the land becomes an illegal target the whole ability does nothing.
 */
val AvatarKyoshiEarthbender = card("Avatar Kyoshi, Earthbender") {
    manaCost = "{5}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Avatar"
    power = 6
    toughness = 6
    oracleText = "During your turn, Avatar Kyoshi has hexproof.\n" +
        "At the beginning of combat on your turn, earthbend 8, then untap that land. (Target land you " +
        "control becomes a 0/0 creature with haste that's still a land. Put eight +1/+1 counters on it. " +
        "When it dies or is exiled, return it to the battlefield tapped.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(8, land) then Effects.Untap(land)
        description = "At the beginning of combat on your turn, earthbend 8, then untap that land."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "130"
        artist = "Tetsuko"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5505044-d181-4af7-bd24-1a703e58d400.jpg?1783904817"
        ruling("2025-10-02", "If the targeted land becomes an illegal target before the spell or ability that includes earthbend resolves, earthbend does nothing. If the spell or ability didn't have other targets, it won't resolve.")
        ruling("2025-10-02", "You may target a land that is already a creature, perhaps because of a previous earthbend ability. The land will get the +1/+1 counters, gain haste, and have its base power and toughness set to 0/0.")
        ruling("2025-10-02", "If a land was animated by earthbend and would go to any zone other than the graveyard or exile, it will not be returned to the battlefield by the delayed triggered ability created by earthbend.")
    }
}
