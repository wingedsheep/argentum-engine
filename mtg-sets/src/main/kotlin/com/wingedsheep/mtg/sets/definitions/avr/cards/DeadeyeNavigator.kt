package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deadeye Navigator
 * {4}{U}{U}
 * Creature — Spirit
 * 5/5
 * Soulbond (You may pair this creature with another unpaired creature when either enters. They
 * remain paired for as long as you control both of them.)
 * As long as Deadeye Navigator is paired with another creature, each of those creatures has
 * "{1}{U}: Exile this creature, then return it to the battlefield under your control."
 *
 * "Each of those creatures" is `GroupFilter.soulbondPair()` — the Navigator *and* its partner, and
 * nobody at all while unpaired (CR 702.95b/e). The granted ability's [EffectTarget.Self] binds to
 * whichever half it was activated on, so blinking through the partner blinks the partner: the
 * engine activates a granted ability with the *receiving* permanent as its source (CR 113.7).
 *
 * The blink is the standard exile-then-return-immediately pair, the same shape as
 * [RestorationAngel]. Blinking either half breaks the pair (the creature left the battlefield, CR
 * 702.95e) and the returning creature is a new object — so the Navigator's soulbond re-triggers on
 * whichever half re-enters, which is exactly the printed loop players use it for.
 */
val DeadeyeNavigator = card("Deadeye Navigator") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as Deadeye Navigator is paired with another creature, each of those creatures has " +
        "\"{1}{U}: Exile this creature, then return it to the battlefield under your control.\""
    power = 5
    toughness = 5

    soulbond()

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Mana("{1}{U}"),
                effect = Effects.Move(EffectTarget.Self, Zone.EXILE)
                    .then(Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD)),
                descriptionOverride = "Exile this creature, then return it to the battlefield under your control"
            ),
            filter = GroupFilter.soulbondPair()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa94262b-f740-48fb-a937-75776864c9ee.jpg?1783940723"
    }
}
