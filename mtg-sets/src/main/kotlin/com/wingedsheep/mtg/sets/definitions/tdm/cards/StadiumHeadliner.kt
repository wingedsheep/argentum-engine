package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Stadium Headliner — Tarkir: Dragonstorm #122
 * {R} · Creature — Goblin Warrior · 1/1
 *
 * Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior
 * creature token. Sacrifice it at the beginning of the next end step.)
 * {1}{R}, Sacrifice this creature: It deals damage equal to the number of creatures you control
 * to target creature.
 *
 * Mobilize is the `mobilize(n)` builder helper (display keyword + attack-triggered token ability).
 *
 * The activated ability sacrifices Stadium Headliner as a cost, so it is no longer a creature you
 * control when the ability resolves — the `DynamicAmount.Count(creatures you control)` is evaluated
 * at resolution and does not include the (already sacrificed) source. Damage is attributed to
 * Stadium Headliner via the ability's source (last-known information), so no `damageSource` override
 * is needed.
 */
val StadiumHeadliner = card("Stadium Headliner") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Mobilize 1 (Whenever this creature attacks, create a tapped and attacking 1/1 red Warrior " +
        "creature token. Sacrifice it at the beginning of the next end step.)\n" +
        "{1}{R}, Sacrifice this creature: It deals damage equal to the number of creatures you control to target creature."

    mobilize(1)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.SacrificeSelf)
        val t = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(
            DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, Filters.Creature),
            t
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Ralph Horsley"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37d4ab2a-a06a-4768-b5e1-e1def957d7f4.jpg?1743204451"
    }
}
