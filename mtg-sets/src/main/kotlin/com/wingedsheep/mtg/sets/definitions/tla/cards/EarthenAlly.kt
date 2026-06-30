package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Earthen Ally
 * {G}
 * Creature — Human Soldier Ally
 * 0/2
 *
 * This creature gets +1/+0 for each color among Allies you control.
 * {2}{W}{U}{B}{R}{G}: Earthbend 5. (Target land you control becomes a 0/0 creature with haste
 * that's still a land. Put five +1/+1 counters on it. When it dies or is exiled, return it to the
 * battlefield tapped.)
 *
 * The self-buff is the standard "gets +X/+0 where X is a dynamic amount" static (Tempest Djinn
 * shape): a Layer 7c [GrantDynamicStatsEffect] bonus over [GroupFilter.source], with `powerBonus`
 * = the number of distinct colors among Allies you control. That count reuses the generic
 * `AggregateBattlefield(DISTINCT_COLORS)` aggregate (via [DynamicAmounts.colorsAmongPermanents]),
 * which reads each creature's projected colors — so Earthen Ally (a green Ally) counts its own
 * green, and a multicolored Ally contributes every one of its colors to the distinct set.
 *
 * Earthbend is the keyword *action* composed from existing primitives via [Effects.Earthbend]
 * (animate land + haste + counters + the two return self-triggers); it rides a plain instant-speed
 * activated ability here — unlike Ba Sing Se, the oracle carries no "Activate only as a sorcery."
 */
val EarthenAlly = card("Earthen Ally") {
    manaCost = "{G}"
    colorIdentity = "WUBRG"
    typeLine = "Creature — Human Soldier Ally"
    power = 0
    toughness = 2
    oracleText = "This creature gets +1/+0 for each color among Allies you control.\n" +
        "{2}{W}{U}{B}{R}{G}: Earthbend 5. (Target land you control becomes a 0/0 creature with " +
        "haste that's still a land. Put five +1/+1 counters on it. When it dies or is exiled, " +
        "return it to the battlefield tapped.)"

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.colorsAmongPermanents(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.ALLY)
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}{U}{B}{R}{G}")
        val land = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(5, land)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Boell Oyino"
        flavorText = "\"Together, we are unshakable.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7a0f2d9-efa4-4a53-a24b-36694457bd1c.jpg?1764121202"
    }
}
