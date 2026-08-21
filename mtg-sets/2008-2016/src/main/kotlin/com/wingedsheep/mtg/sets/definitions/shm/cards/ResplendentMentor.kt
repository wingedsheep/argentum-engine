package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Resplendent Mentor
 * {4}{W}
 * Creature — Kithkin Cleric
 * 2 / 2
 *
 * White creatures you control have "{T}: You gain 1 life."
 *
 * - The quoted ability is a real granted [ActivatedAbility] ([GrantActivatedAbility] over a group,
 *   like Citanul Hierophants), not a keyword or a trigger: each white creature gets its own
 *   instance, so each can be tapped for the life independently and is summoning-sickness-gated on
 *   its own.
 * - No "Other": the Mentor is white itself, so the [GroupFilter] deliberately has no `excludeSelf`
 *   and it gains the ability as well.
 * - "You" in the granted ability is the ability's controller, which is [Effects.GainLife]'s default
 *   `EffectTarget.Controller` — so a creature that changes controller gains life for its *new*
 *   controller.
 */
val ResplendentMentor = card("Resplendent Mentor") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Kithkin Cleric"
    power = 2
    toughness = 2
    oracleText = "White creatures you control have \"{T}: You gain 1 life.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.GainLife(1)
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Franz Vohwinkel"
        flavorText = "Thoughtweft gives new meaning to the phrase \"common knowledge.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be87e59d-422c-4dd7-8867-423e784830a2.jpg?1783942766"
    }
}
