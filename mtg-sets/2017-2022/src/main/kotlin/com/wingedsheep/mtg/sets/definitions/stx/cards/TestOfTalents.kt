package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Test of Talents — Strixhaven: School of Mages #59 (canonical printing)
 * {1}{U} · Instant
 *
 * Counter target instant or sorcery spell. Search its controller's graveyard, hand, and library
 * for any number of cards with the same name as that spell and exile them. That player shuffles,
 * then draws a card for each card exiled from their hand this way.
 *
 * The End's search-and-exile pipeline, fronted by a counter instead of an exile. The spell's name
 * and controller are captured while it is still on the stack — the counter moves the card to its
 * *owner's* graveyard (and a countered copy ceases to exist), so neither can be read afterwards.
 * The countered card is then among the graveyard matches, free to exile (ruling). Inside
 * `forEachCaptured`, `Player.You` / `EffectTarget.Controller` are the spell's controller while
 * [Chooser.SourceController] keeps Test of Talents' caster making the choices.
 */
val TestOfTalents = card("Test of Talents") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target instant or sorcery spell. Search its controller's graveyard, hand, " +
        "and library for any number of cards with the same name as that spell and exile them. That " +
        "player shuffles, then draws a card for each card exiled from their hand this way."

    spell {
        target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.Pipeline {
            val spell = gather(CardSource.ChosenTargets, name = "testedSpell")
            val spellControllers = captureControllers(spell, name = "testedSpellControllers")
            val spellName = storeCardName(spell, name = "testedSpellName")
            run(Effects.CounterSpell())

            forEachCaptured(spell, spell, spellControllers) {
                val matches = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = Player.You,
                        filter = GameObjectFilter.Any.namedFromVariable(spellName)
                    ),
                    name = "testedMatches",
                    search = true
                )
                val selected = chooseAnyNumber(
                    from = matches,
                    chooser = Chooser.SourceController,
                    prompt = "Choose any number of cards with that name to exile",
                    showAllCards = true,
                    alwaysPrompt = true,
                    name = "testedSelected"
                )
                val selectedFromHand = filter(
                    selected,
                    GameObjectFilter.Any.currentlyIn(Zone.HAND),
                    name = "testedSelectedFromHand"
                )
                exile(selected)
                run(Effects.ShuffleLibrary(target = EffectTarget.Controller))
                run(Effects.DrawCards(selectedFromHand.count, EffectTarget.Controller))
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e2b6236-b40c-430c-98b0-7940b942657a.jpg?1783927374"
        ruling("2021-04-16", "Countering a spell normally puts that card into the graveyard, so that card will be there to find and exile.")
        ruling("2021-04-16", "If the back face of a modal double-faced card is countered, you will not be able to exile any cards, including the one that you countered, because those cards have only their front-face characteristics (including name) in the graveyard, hand, and library.")
    }
}
