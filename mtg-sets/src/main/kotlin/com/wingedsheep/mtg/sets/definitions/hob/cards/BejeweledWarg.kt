package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Bejeweled Warg — The Hobbit #117
 * {1}{G} · Creature — Wolf · Rare
 * 3/2
 *
 * Trample
 * Whenever this creature deals combat damage to a player, choose one —
 * • Put a +1/+1 counter on target Wolf you control.
 * • Create a Treasure token.
 *
 * Modeling notes:
 *  - The mode is chosen as the trigger goes on the stack, and only the counter mode targets
 *    (CR 603.3d) — so with no Wolf you control the player is left with the Treasure mode. Bejeweled
 *    Warg is itself a Wolf, so it is normally its own legal target.
 *  - `countsAsModalSpell = false`: this is a modal *ability*, not a modal spell, so it must not feed
 *    `SpellCastEvent.chosenModesCount` / `SpellCastPredicate.IsModal`.
 */
val BejeweledWarg = card("Bejeweled Warg") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    power = 3
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, choose one —\n" +
        "• Put a +1/+1 counter on target Wolf you control.\n" +
        "• Create a Treasure token."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.WOLF)),
                "Put a +1/+1 counter on target Wolf you control"
            ),
            Mode.noTarget(
                Effects.CreateTreasure(1),
                "Create a Treasure token"
            ),
            countsAsModalSpell = false
        )
        description = "Whenever Bejeweled Warg deals combat damage to a player, choose one — " +
            "put a +1/+1 counter on target Wolf you control; or create a Treasure token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "John Di Giovanni"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e95eba5c-e0d6-46b4-a0be-8e373b2185ea.jpg?1785496330"
    }
}
