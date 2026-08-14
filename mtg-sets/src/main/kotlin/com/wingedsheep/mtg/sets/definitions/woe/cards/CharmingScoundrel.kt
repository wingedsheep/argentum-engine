package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
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
 * Charming Scoundrel
 * {1}{R}
 * Creature — Human Rogue (1/1)
 *
 * Haste
 * When this creature enters, choose one —
 * • Discard a card, then draw a card.
 * • Create a Treasure token.
 * • Create a Wicked Role token attached to target creature you control.
 *
 * A three-mode ETB via [ModalEffect.chooseOne]. Only the third mode targets, so it uses
 * [Mode.withTarget]; the loot and Treasure modes are [Mode.noTarget]. The loot mode is discard-then-
 * draw (order matters — discard resolves before the draw). The Wicked Role token carries the +1/+1
 * buff and the "put into a graveyard, each opponent loses 1 life" trigger; [Effects.CreateRoleToken]
 * handles replacing an existing Role you control on that creature.
 */
val CharmingScoundrel = card("Charming Scoundrel") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "When this creature enters, choose one —\n" +
        "• Discard a card, then draw a card.\n" +
        "• Create a Treasure token.\n" +
        "• Create a Wicked Role token attached to target creature you control."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.Discard(1).then(Effects.DrawCards(1)),
                "Discard a card, then draw a card."
            ),
            Mode.noTarget(
                Effects.CreateTreasure(),
                "Create a Treasure token."
            ),
            Mode.withTarget(
                Effects.CreateRoleToken("Wicked Role", EffectTarget.ContextTarget(0)),
                TargetCreature(filter = TargetFilter.CreatureYouControl),
                "Create a Wicked Role token attached to target creature you control."
            )
        )
        description = "When this creature enters, choose one."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8090bcf-e17a-4110-a518-77ccd045b18f.jpg?1783915097"
    }
}
