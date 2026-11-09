package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasive Surgery — Shadows over Innistrad #68
 * {U} · Instant
 *
 * Counter target sorcery spell.
 * Delirium — If there are four or more card types among cards in your graveyard, search the
 * graveyard, hand, and library of that spell's controller for any number of cards with the same
 * name as that spell, exile those cards, then that player shuffles.
 *
 * The spell's name and controller are snapshotted *before* the counter, while it is still a spell
 * on the stack (Tibalt's Trickery's shape): once countered it is a card in its owner's graveyard,
 * and a spell cast by a non-owner would read back as its owner's. Delirium is checked *after* the
 * counter (ruling), so a sorcery of your own you countered counts toward it — and the countered
 * card, now in its graveyard, is one of the cards the search can find.
 *
 * The search runs over the countered spell's controller ([Effects.ForEachPlayer] over the
 * snapshot, so "their graveyard, hand, and library" and "that player shuffles" are theirs), while
 * Invasive Surgery's controller is the one searching and picking ([Player.ControllerOfSource] /
 * [Chooser.SourceController]). The pick decision shows the searcher the matches; no look is
 * recorded for the iterated player. "Any number" lets the searcher fail to find.
 */
val InvasiveSurgery = card("Invasive Surgery") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target sorcery spell.\nDelirium — If there are four or more card types among " +
        "cards in your graveyard, search the graveyard, hand, and library of that spell's controller for " +
        "any number of cards with the same name as that spell, exile those cards, then that player shuffles."

    spell {
        target(TargetFilter.SorcerySpellOnStack)
        effect = Effects.Pipeline {
            val spell = gather(CardSource.ChosenTargets)
            val spellName = storeCardName(spell)
            val itsController = storePlayer(Player.ControllerOf("target spell"))
            run(Effects.CounterSpell())
            ifThen(Conditions.Delirium()) {
                run(
                    Effects.ForEachPlayer(
                        itsController.asPlayers,
                        Effects.Pipeline {
                            val matches = gather(
                                CardSource.FromMultipleZones(
                                    zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                                    player = Player.You,
                                    filter = GameObjectFilter.Any.namedFromVariable(spellName)
                                ),
                                search = true,
                                searcher = Player.ControllerOfSource,
                                lookAudience = LookAudience.None
                            )
                            val toExile = chooseAnyNumber(
                                from = matches,
                                chooser = Chooser.SourceController,
                                prompt = "Choose any number of cards with that name to exile",
                                selectedLabel = "Exile"
                            )
                            exile(toExile)
                            run(Effects.ShuffleLibrary())
                        }
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e644e38-39bf-40bd-9be1-5eb80f472e81.jpg?1783937797"
        ruling(
            "2016-04-08",
            "Invasive Surgery's delirium ability isn't checked until after the sorcery spell has been " +
                "countered. If that spell is put into your graveyard, it will be counted."
        )
    }
}
