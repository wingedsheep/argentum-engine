package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Failure // Comply — Amonkhet #221
 *
 * Failure {1}{U} — Instant: Return target spell to its owner's hand.
 * Comply {W} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Choose a card name. Until your next turn, your opponents can't cast spells with the chosen name.
 *
 * Comply's name is chosen on resolution from any card name (lands included, per the rulings —
 * they can still be played) and frozen into the ban by [Effects.CantCastSpells].
 */
val FailureComply = card("Failure // Comply") {
    layout = CardLayout.SPLIT
    colorIdentity = "WU"

    face("Failure") {
        manaCost = "{1}{U}"
        typeLine = "Instant"
        oracleText = "Return target spell to its owner's hand."

        spell {
            target(TargetFilter.SpellOnStack)
            effect = Effects.ReturnSpellToOwnersHand()
        }
    }

    face("Comply") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Choose a card name. Until your next turn, your opponents can't cast spells with the chosen name."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.Pipeline {
                val chosenName = chooseCardName(
                    prompt = "Choose a card name",
                    pool = CardNamePool.ANY
                )
                run(
                    Effects.CantCastSpells(
                        target = EffectTarget.PlayerRef(Player.EachOpponent),
                        duration = Duration.UntilYourNextTurn,
                        spellFilter = GameObjectFilter.Any.namedFromVariable(chosenName)
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9623c8c-01b4-4e8f-a5b9-eeea408ec027.jpg?1783936454"
        ruling("2017-04-18", "If a spell is returned to its owner's hand, it's removed from the stack and thus will not resolve. The spell isn't countered; it just no longer exists. This works against a spell that can't be countered.")
        ruling("2017-04-18", "If a copy of a spell is returned to its owner's hand, it's moved there, then it will cease to exist as a state-based action.")
        ruling("2017-04-18", "You choose a card name as Comply resolves, not as you cast it. Opponents can't cast spells with that name after you've chosen it.")
        ruling("2017-04-18", "You may choose the name of a land card. Cards with that name can still be played since lands are never cast.")
        ruling("2017-04-18", "If you choose the name of a split card, you choose one name, not both. For example, you could name Failure or Comply, but not Failure // Comply. Opponents are still allowed to cast the half you didn't choose.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
