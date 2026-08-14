package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Polygraph Orb — Murders at Karlov Manor #99
 * {4}{B} · Artifact
 *
 * When this artifact enters, look at the top four cards of your library. Put two of them into your
 * hand and the rest into your graveyard. You lose 2 life.
 * {2}, {T}, Collect evidence 3: Each opponent loses 3 life unless they discard a card or sacrifice
 * a creature.
 *
 * The entry clause is the Resentful Revelation shape at a bigger size —
 * [Patterns.Library.lookAtTopAndKeep] with `count = 4, keepCount = 2`. The two halves aren't
 * symmetric: keeping is `ChooseExactly(2)`, so with a library of one or two cards you keep what
 * there is and nothing is milled; the life loss is unconditional either way and happens whether or
 * not anything was found.
 *
 * The activated ability's "unless" is a **choice made by each opponent**, not a cost the Orb's
 * controller can pay, so it is a [ChooseActionEffect] inside a `ForEachPlayerEffect(EachOpponent)` —
 * inside the iteration the controller is rebound to the iterated opponent, so `EffectTarget.Controller`
 * is that opponent. The discard and sacrifice options are feasibility-gated (empty hand, no
 * creatures) and hide themselves; "lose 3 life" carries **no** gate, because the ruling is explicit
 * that an opponent may always take the life loss even holding cards and creatures. That makes it
 * genuinely a punisher rather than an edict.
 *
 * `Costs.CollectEvidence(3)` is a real cost atom sitting alongside `{2}` and `{T}`, which is what
 * makes the official ruling fall out: costs are paid during activation, so opponents get no window
 * to strand the evidence by emptying your graveyard, and if you can't exile cards totalling mana
 * value 3 the ability simply can't be activated.
 */
val PolygraphOrb = card("Polygraph Orb") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, look at the top four cards of your library. Put two of " +
        "them into your hand and the rest into your graveyard. You lose 2 life.\n" +
        "{2}, {T}, Collect evidence 3: Each opponent loses 3 life unless they discard a card or " +
        "sacrifice a creature. (To collect evidence 3, exile cards with total mana value 3 or " +
        "greater from your graveyard.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Patterns.Library.lookAtTopAndKeep(count = 4, keepCount = 2),
            Effects.LoseLife(2, EffectTarget.Controller)
        )
        description = "When this artifact enters, look at the top four cards of your library. Put " +
            "two of them into your hand and the rest into your graveyard. You lose 2 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.CollectEvidence(3))
        effect = ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = listOf(
                ChooseActionEffect(
                    player = EffectTarget.Controller,
                    choices = listOf(
                        EffectChoice(
                            label = "Discard a card",
                            effect = Patterns.Hand.discardCards(1, EffectTarget.Controller),
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND),
                        ),
                        EffectChoice(
                            label = "Sacrifice a creature",
                            effect = ForceSacrificeEffect(
                                filter = GameObjectFilter.Creature,
                                count = 1,
                                target = EffectTarget.Controller,
                            ),
                            feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                                GameObjectFilter.Creature
                            ),
                        ),
                        // No feasibility gate: "Your opponent can always choose to lose 3 life,
                        // even if they have cards to discard or creatures to sacrifice."
                        EffectChoice(
                            label = "Lose 3 life",
                            effect = Effects.LoseLife(3, EffectTarget.Controller),
                        ),
                    ),
                )
            ),
        )
        description = "Each opponent loses 3 life unless they discard a card or sacrifice a creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6cc4c6f-4a84-4d42-89fa-7405f7ad6ba0.jpg?1783912893"

        ruling(
            "2024-02-02",
            "While resolving Polygraph Orb's last ability, your opponent chooses a card to be " +
                "discarded without revealing it, chooses a creature to be sacrificed, or chooses to " +
                "do neither. Then that player discards that card, sacrifices that creature, or " +
                "loses 3 life. Your opponent can always choose to lose 3 life, even if they have " +
                "cards to discard or creatures to sacrifice."
        )
        ruling(
            "2024-02-02",
            "In a multiplayer game, each opponent in turn order makes their choice once, then all " +
                "of the actions occur simultaneously."
        )
        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you can't " +
                "choose to collect evidence at all."
        )
        ruling(
            "2024-02-02",
            "Once you've announced that you're casting a spell, players can't take actions until " +
                "you've finished doing so. Notably, opponents can't try to remove cards from your " +
                "graveyard to stop you from collecting evidence."
        )
    }
}
