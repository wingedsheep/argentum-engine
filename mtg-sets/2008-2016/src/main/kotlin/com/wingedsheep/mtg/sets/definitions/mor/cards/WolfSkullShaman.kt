package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wolf-Skull Shaman — Morningtide #140
 * {1}{G} · Creature — Elf Shaman · 2/2
 *
 * Kinship — At the beginning of your upkeep, you may look at the top card of your library. If it
 * shares a creature type with this creature, you may reveal it. If you do, create a 2/2 green Wolf
 * creature token.
 *
 * Kinship is an ability word with no rules of its own, so it composes from the peek pipeline:
 *  1. "You may look" — the whole resolution sits behind an [Effects.May]; declining looks at nothing.
 *  2. Gather the top card as a peek (it never leaves the library — it stays on top afterwards, per
 *     the rulings).
 *  3. Offer up to one card that shares a creature type with the Shaman
 *     (`sharingCreatureTypeWith(EffectTarget.Self)`, projected types on the Shaman's side) — the
 *     player may decline even when it matches (ruling). `showAllCards` keeps a non-matching card
 *     visible, since the player did look at it.
 *  4. Only if a card was chosen: reveal it, then create the Wolf ("If you do").
 */
val WolfSkullShaman = card("Wolf-Skull Shaman") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Shaman"
    power = 2
    toughness = 2
    oracleText = "Kinship — At the beginning of your upkeep, you may look at the top card of your " +
        "library. If it shares a creature type with this creature, you may reveal it. If you do, " +
        "create a 2/2 green Wolf creature token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(1))
                val (revealed, _) = chooseUpToSplit(
                    1,
                    from = looked,
                    filter = GameObjectFilter.Any.sharingCreatureTypeWith(EffectTarget.Self),
                    prompt = "Reveal it to create a 2/2 green Wolf creature token?",
                    selectedLabel = "Reveal",
                    remainderLabel = "Keep it hidden",
                    showAllCards = true,
                )
                ifNotEmpty(revealed) {
                    reveal(revealed)
                    run(
                        Effects.CreateToken(
                            power = 2,
                            toughness = 2,
                            colors = setOf(Color.GREEN),
                            creatureTypes = setOf(Subtype.WOLF.value),
                            imageUri = "https://cards.scryfall.io/normal/front/0/2/02a80dc2-0811-4df0-95f1-dd80ce9e24de.jpg?1783942837",
                        )
                    )
                }
            },
            prompt = "Look at the top card of your library?",
        )
        description = "Kinship — At the beginning of your upkeep, you may look at the top card of " +
            "your library. If it shares a creature type with this creature, you may reveal it. If " +
            "you do, create a 2/2 green Wolf creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Jim Murray"
        flavorText = "A chorus of howls answers his call."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/804ef999-0d44-420e-ad0c-a6dcf8e9c392.jpg?1783942775"
        ruling(
            "2008-04-01",
            "You don't have to reveal the top card of your library, even if it shares a creature " +
                "type with the creature that has the kinship ability."
        )
        ruling(
            "2008-04-01",
            "After the kinship ability finishes resolving, the card you looked at remains on top of " +
                "your library."
        )
        ruling(
            "2008-04-01",
            "If you have multiple creatures with kinship abilities, each triggers and resolves " +
                "separately. You'll look at the same card for each one, unless you have some method " +
                "of shuffling your library or moving that card to a different zone."
        )
    }
}
