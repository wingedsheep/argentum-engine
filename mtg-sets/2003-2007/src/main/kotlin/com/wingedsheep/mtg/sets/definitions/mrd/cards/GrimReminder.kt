package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.namedFromVariable
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Reminder — Mirrodin #66 (canonical printing)
 * {2}{B} · Instant
 *
 * Search your library for a nonland card and reveal it. Each opponent who cast a spell this turn
 * with the same name as that card loses 6 life. Then shuffle.
 * {B}{B}: Return this card from your graveyard to your hand. Activate only during your upkeep.
 *
 * A card that reads the *past*: nothing on the battlefield matters, only what each opponent has
 * already cast this turn. So the searched card is never moved — it is revealed to name a card, the
 * name is captured into the pipeline, and then each opponent's own cast history is asked whether it
 * contains a spell of that name.
 *
 * That last step is the part the engine could not do before. `GameState.spellsCastThisTurnByPlayer`
 * has recorded each cast spell's *name* all along, and `CardPredicate.NameEquals` matched against
 * it — but `NameEqualsChosen`, the predicate that reads a name captured at resolution time, was
 * hardcoded to `false` on the record path, and the record matcher took no predicate context to read
 * one from. Both are fixed here, so `namedFromVariable` now means the same thing against cast
 * history as it does against cards in a zone.
 *
 * Two details of the search that the shape has to honour:
 * - It is `chooseUpTo(1)`, not `chooseExactly(1)`. The library is a hidden zone, so a player may
 *   always fail to find (CR 701.19c) — and an empty library must not deadlock the spell.
 * - Failing to find stores no name, so no `NameEqualsChosen` can match and nobody loses life. The
 *   shuffle still happens, because "Then shuffle" is not conditional on finding anything.
 *
 * The second ability is the ordinary graveyard-activation shape (Undead Gladiator's template):
 * `activateFromZone = Zone.GRAVEYARD` plus the your-turn/upkeep restriction pair — which is what
 * makes the card a recurring threat rather than a one-shot, since a returned Grim Reminder is a
 * card your opponent has now seen and must play around.
 */
val GrimReminder = card("Grim Reminder") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Search your library for a nonland card and reveal it. Each opponent who cast a " +
        "spell this turn with the same name as that card loses 6 life. Then shuffle.\n" +
        "{B}{B}: Return this card from your graveyard to your hand. Activate only during your upkeep."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Nonland)
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for a nonland card"
            )
            reveal(found, revealToSelf = false)
            val cardName = storeCardName(found, name = "grimName")

            // Player.You inside the loop is the opponent being processed, so the condition asks
            // that opponent's own cast history. No name captured (failed to find, or an empty
            // library) means the predicate matches nothing and no one loses life.
            run(
                Effects.ForEachPlayer(
                    Player.EachOpponent,
                    listOf(
                        ConditionalEffect(
                            Conditions.YouCastSpellsThisTurn(
                                atLeast = 1,
                                filter = GameObjectFilter.Any.namedFromVariable(cardName.key)
                            ),
                            Effects.LoseLife(6, EffectTarget.PlayerRef(Player.You))
                        )
                    )
                )
            )

            run(ShuffleLibraryEffect())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    activatedAbility {
        cost = Costs.Mana("{B}{B}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
        description = "Return this card from your graveyard to your hand. " +
            "Activate only during your upkeep."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35baa726-8c2f-4a0b-93d1-d7ecfae69fe4.jpg?1783944547"
    }
}
