package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Quicksilver Elemental — Mirrodin #47
 * {3}{U}{U} · Creature — Elemental · Rare · 3/4
 *
 * {U}: This creature gains all activated abilities of target creature until end of turn.
 * (If any of the abilities use that creature's name, use this creature's name instead.)
 * You may spend blue mana as though it were mana of any color to pay the activation costs of
 * this creature's abilities.
 *
 * Modelling notes:
 * - The first ability is [Effects.GainAllActivatedAbilitiesOf] with the target as the donor and the
 *   default `EffectTarget.Self` as the receiver. The set of abilities is snapshotted as the ability
 *   resolves, so the target later gaining abilities or leaving the battlefield changes nothing —
 *   the reading the Havengul Lich ruling pins for this wording. That is also why it is an effect
 *   rather than the continuously re-read static `GainActivatedAbilitiesOfPermanents` (Sharkey,
 *   Marvin).
 * - Each gained ability is granted with Quicksilver Elemental as its source (CR 113.7), which *is*
 *   the parenthetical reminder: a copied "Sacrifice a creature: Nantuko Husk gets +2/+2" sacrifices
 *   to, and pumps, the Elemental.
 * - "You can activate the ability more than once, collecting abilities from multiple creatures (or
 *   the same creature more than once)" falls out of the grants simply accumulating, and the
 *   executor's donor-derived ability ids keep two donors that share a card definition from
 *   collapsing into one gained ability.
 * - The second ability is the *narrow* arm of [SpendAnyManaTypeForActivatedAbilities]: only blue
 *   mana is substitutable, and only for colored requirements ("as though it were mana of any
 *   **color**"), so a gained `{2}{G}` ability still needs two generic from anywhere and one mana
 *   that is green or blue. `GroupFilter.source()` scopes it to "this creature's abilities", which
 *   covers the gained ones because a granted ability's source is the permanent that has it.
 */
val QuicksilverElemental = card("Quicksilver Elemental") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 4
    oracleText = "{U}: This creature gains all activated abilities of target creature until end of " +
        "turn. (If any of the abilities use that creature's name, use this creature's name " +
        "instead.)\n" +
        "You may spend blue mana as though it were mana of any color to pay the activation costs " +
        "of this creature's abilities."

    // "{U}: This creature gains all activated abilities of target creature until end of turn."
    activatedAbility {
        cost = Costs.Mana("{U}")
        val donor = target("target creature", Targets.Creature)
        effect = Effects.GainAllActivatedAbilitiesOf(donor)
        description = "{U}: This creature gains all activated abilities of target creature until end of turn."
    }

    // "You may spend blue mana as though it were mana of any color to pay the activation costs of
    //  this creature's abilities."
    staticAbility {
        ability = SpendAnyManaTypeForActivatedAbilities(
            filter = GroupFilter.source(),
            substituteColor = Color.BLUE
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2905f6ac-d054-454b-8e1a-9c32db13a581.jpg?1783944552"
    }
}
