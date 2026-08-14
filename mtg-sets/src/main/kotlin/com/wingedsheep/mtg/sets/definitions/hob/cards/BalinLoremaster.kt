package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Balin, Loremaster
 * {3}{R}{R}
 * Legendary Creature — Dwarf Bard
 * 4/4
 *
 * Storied.
 * Whenever Balin or another Dwarf you control enters, you may discard your hand. Draw X cards,
 * where X is the number of cards discarded this way. If you have an enduring story, Balin deals
 * X damage to each opponent.
 *
 * "Balin or another Dwarf you control" is the Fíli/Arahbo shape: a single `ANY`-bound enters
 * trigger over `Dwarf.youControl()` covers both halves, because Balin is himself a Dwarf you
 * control and so matches his own filter.
 *
 * X is *"discarded this way"*, not "your hand size" — so it has to be the size of the collection
 * the discard actually moved, read back as `discardedHand_count`.
 * [Patterns.Hand.discardHand] gathers the hand into `discardedHand` before discarding it and
 * `GatherCardsEffect` auto-publishes the `_count` companion (the Borrowed Knowledge idiom).
 *
 * Only the *discard* is optional; the draw and the damage happen either way. Modelling the "may"
 * as a [Gate.MayDecide] around just the discard — rather than around the whole rider — is what
 * makes a decline read X = 0 instead of skipping the rest of the ability. That works because an
 * unset `VariableReference` evaluates to 0, and because pipeline storage survives the decision
 * pause the gate introduces — the gated resumer hands the taken branch's collections to the frame
 * beneath via `exposeCollectionsToNextFrame`, which this card is the first to depend on. Drawing 0
 * and dealing 0 damage are both no-ops, so a decline is correctly indistinguishable from discarding
 * an empty hand.
 *
 * The enduring-story clause is a resolution-time state test ([ConditionalEffect] → a
 * `Gate.WhenCondition`), not an intervening-if: the ability triggers and goes on the stack
 * regardless, and only the damage half checks the designation as it resolves.
 */
val BalinLoremaster = card("Balin, Loremaster") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Bard"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "Whenever Balin or another Dwarf you control enters, you may discard your hand. Draw X " +
        "cards, where X is the number of cards discarded this way. If you have an enduring story, " +
        "Balin deals X damage to each opponent."
    power = 4
    toughness = 4

    storied()

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.DWARF).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            GatedEffect(
                gate = Gate.MayDecide(),
                then = Patterns.Hand.discardHand(),
                descriptionOverride = "You may discard your hand.",
            ),
            Effects.DrawCards(DynamicAmount.VariableReference("discardedHand_count")),
            ConditionalEffect(
                condition = Conditions.YouHaveEnduringStory,
                effect = DealDamageEffect(
                    amount = DynamicAmount.VariableReference("discardedHand_count"),
                    target = EffectTarget.PlayerRef(Player.EachOpponent),
                ),
            ),
        )
        description = "Whenever Balin or another Dwarf you control enters, you may discard your " +
            "hand. Draw X cards, where X is the number of cards discarded this way. If you have " +
            "an enduring story, Balin deals X damage to each opponent."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42d7ca7b-c983-40fd-ad57-59f6972bb375.jpg?1785496498"
    }
}
