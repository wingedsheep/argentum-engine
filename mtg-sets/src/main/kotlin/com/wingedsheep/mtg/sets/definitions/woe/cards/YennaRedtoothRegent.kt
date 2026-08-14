package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Yenna, Redtooth Regent
 * {2}{G}{W}
 * Legendary Creature — Elf Noble
 * 4/4
 *
 * {2}, {T}: Choose target enchantment you control that doesn't have the same name as another
 * permanent you control. Create a token that's a copy of it, except it isn't legendary. If the
 * token is an Aura, untap Yenna, then scry 2. Activate only as a sorcery.
 *
 * Implementation notes:
 *  - **The name restriction** is the target filter, not a resolution check: "doesn't have the same
 *    name as another permanent you control" is
 *    [com.wingedsheep.sdk.scripting.predicates.CardPredicate.NameNotSharedWithAnotherControlledPermanent].
 *    It compares against every *other* permanent the controller has out — the token Yenna just
 *    made included — so activating twice on the same enchantment is illegal the second time, which
 *    is the card's whole design constraint.
 *  - **"except it isn't legendary"** is `removedSupertypes = {LEGENDARY}` on
 *    [Effects.CreateTokenCopyOfTarget]; the copy exception is baked into the token's own copiable
 *    values, so anything that later copies the token also isn't legendary (printed ruling).
 *  - **Aura copies** are handled generically by the token-copy executor: a token copy of an Aura
 *    is created rather than cast, so its controller chooses what it enchants as it enters
 *    (CR 303.4h), bound by the copied Aura's own enchant restriction. See
 *    `AuraTokenHostChooser`.
 *  - **The Aura rider** branches on the chosen target rather than on the created token. The two
 *    coincide except when the Aura token can't be created for want of a legal host (CR 303.4g) —
 *    a corner that can only arise if every object the Aura could enchant has left the battlefield
 *    since activation, since the Aura's current host is itself always a legal choice.
 */
val YennaRedtoothRegent = card("Yenna, Redtooth Regent") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Noble"
    power = 4
    toughness = 4
    oracleText = "{2}, {T}: Choose target enchantment you control that doesn't have the same name " +
        "as another permanent you control. Create a token that's a copy of it, except it isn't " +
        "legendary. If the token is an Aura, untap Yenna, then scry 2. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val enchantment = target(
            "enchantment you control that doesn't have the same name as another permanent you control",
            TargetPermanent(
                filter = TargetFilter(
                    GameObjectFilter.Enchantment
                        .youControl()
                        .nameNotSharedWithAnotherControlledPermanent()
                )
            )
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = enchantment,
            removedSupertypes = setOf(Supertype.LEGENDARY),
        ) then ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Enchantment.withSubtype(Subtype.AURA)
            ),
            effect = Effects.Untap(EffectTarget.Self) then Effects.Scry(2),
        )
        description = "Choose target enchantment you control that doesn't have the same name as " +
            "another permanent you control. Create a token that's a copy of it, except it isn't " +
            "legendary. If the token is an Aura, untap Yenna, then scry 2."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Justyna Dura"
        flavorText = "\"My people are both bloom and thorn.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e635d461-254a-434e-8e5d-dea61dd8ca4f.jpg?1783915066"

        ruling("2023-09-01", "Except for the listed exceptions, the token copies exactly what was printed on the original enchantment and nothing else (unless that enchantment is itself copying something else). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2023-09-01", "If the copied enchantment is copying something else, then the token enters the battlefield as whatever that enchantment copied, with the stated exceptions.")
        ruling("2023-09-01", "If the copied enchantment has {X} in its mana cost, X is 0.")
        ruling("2023-09-01", "Any enters-the-battlefield abilities of the copied enchantment will trigger when the token enters the battlefield. Any \"as [this enchantment] enters the battlefield\" or \"[this enchantment] enters the battlefield with\" abilities of the enchantment will also work.")
        ruling("2023-09-01", "If something becomes a copy of the token, the copy also isn't legendary.")
    }
}
