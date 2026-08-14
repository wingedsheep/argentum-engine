package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ancient Vendetta — Aetherdrift #75
 * {3}{B} · Sorcery
 *
 * Choose a card name. Search target opponent's graveyard, hand, and library for up to four cards
 * with that name and exile them. Then that player shuffles.
 *
 * Lobotomy's shape with two differences that matter:
 *
 *  - The name is **typed, not picked** — [OptionType.CARD_NAME] (Desperate Research's
 *    [Effects.ChooseCardName]) rather than Lobotomy's "reveal their hand, then choose a card from
 *    it". You are naming blind, so this can whiff entirely.
 *  - It exiles **up to four**, not all, so the gathered matches go through a `chooseUpTo(4)`
 *    selection. Four is a ceiling, not a requirement — the controller may exile fewer (or none),
 *    which matters when exiling would turn on an opponent's graveyard-hate or escape payoff.
 *
 * The name is chosen on resolution, not as the spell is cast: the printed text is an ordinary
 * effect, so the opponent sees the spell on the stack before the name exists.
 *
 * The search spans a hidden zone (hand and library both), so the selection is a private look for
 * the controller; the shuffle afterwards is what stops it from also being a free library-order
 * read.
 */
val AncientVendetta = card("Ancient Vendetta") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a card name. Search target opponent's graveyard, hand, and library for " +
        "up to four cards with that name and exile them. Then that player shuffles."

    spell {
        target("opponent", Targets.Opponent)
        effect = Effects.Pipeline {
            // 1. Choose a card name.
            val chosenName = chooseOption(
                OptionType.CARD_NAME,
                prompt = "Choose a card name",
                name = "chosenName"
            )
            // 2. Search that opponent's graveyard, hand, and library for cards with that name.
            val matches = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                    player = Player.ContextPlayer(0),
                    filter = GameObjectFilter.Any.namedFromVariable(chosenName.key)
                ),
                name = "matches"
            )
            // 3. Up to four of them — a ceiling, not a requirement.
            val toExile = chooseUpTo(
                4, from = matches,
                prompt = "Exile up to four cards with the chosen name",
                name = "toExile"
            )
            // 4. Exile them.
            exile(toExile, owner = Player.ContextPlayer(0))
            // 5. Then that player shuffles.
            run(ShuffleLibraryEffect(target = EffectTarget.ContextTarget(0)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Tianxing Xu"
        flavorText = "\"Make peace with your end, for not even your legacy will remain.\"\n—Zahur"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/230301f2-f288-4b13-9f62-e649ad8357bb.jpg?1783907900"
    }
}
