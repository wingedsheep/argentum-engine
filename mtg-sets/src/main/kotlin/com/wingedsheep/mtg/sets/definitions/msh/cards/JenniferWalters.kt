package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.effects.DynamicHint
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Jennifer Walters // The Sensational She-Hulk — Marvel Super Heroes #18 (mythic)
 *
 * Front — Jennifer Walters · {1}{W} · Legendary Creature — Human Advisor Hero · 2/3
 *   Your opponents can't cast spells during your turn.
 *   {3}{G}{W}{W}: Transform Jennifer Walters. Activate only as a sorcery.
 *
 * Back — The Sensational She-Hulk · Legendary Creature — Gamma Hero · 6/6
 *   Reach, trample
 *   Your opponents can't cast spells during your turn.
 *   Whenever a creature you control is dealt damage, you may have The Sensational She-Hulk deal
 *     that much damage to any target. Do this only once each turn.
 *
 * A **modal** double-faced creature ([CardDefinition.modalDoubleFacedPermanent]), the shape the
 * whole MSH hero cycle shares. CR 712.3 allows a modal DFC to also transform, and this card uses
 * both routes to the same back face:
 *
 *  - **Cast it** from hand for its own `{3}{G}{W}{W}` (CR 712.11b — the caster picks a face before
 *    the card goes on the stack; CR 712.11c — only that face is evaluated).
 *  - **Transform into it** with the front's sorcery-speed [TransformEffect] ability.
 *
 * The back therefore carries its printed mana cost and *no* color indicator: its G/W comes from
 * `{3}{G}{W}{W}`, and per CR 712.8f — which, unlike CR 712.8e for nonmodal DFCs, has no mana-value
 * exception — a transformed She-Hulk has mana value 6 rather than Jennifer Walters' 2.
 *
 *  - **"Your opponents can't cast spells during your turn"** (both faces) is the reusable
 *    [PlayersCantCastSpells] static scoped to [Player.EachOpponent] with `condition = IsYourTurn` —
 *    the Voice of Victory precedent. The engine reads it at cast-legality time, so it covers every
 *    casting zone with no per-zone wiring.
 *
 *  - **The damage mirror** is a `DealsDamageEvent` observer ([TriggerBinding.ANY]) filtered to
 *    [RecipientFilter.CreatureYouControl] — the Kazarov, Sengir Pureblood shape read from the
 *    recipient's side. It is deliberately *not* a batch trigger: CR 603.2 makes it fire once per
 *    damaged creature, so a multi-block puts one instance on the stack per creature that was dealt
 *    damage. "That much damage" reads the triggering event's amount via
 *    [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT].
 *
 *  - **"Do this only once each turn" is not the trigger cap** — `effectOncePerTurn = true`, never
 *    `oncePerTurn = true`. That is the whole card. Per CR 603.2h the ability "triggers only if its
 *    source's controller has not yet taken the indicated action that turn": until She-Hulk has
 *    mirrored something, every damaged creature offers its own instance, so the controller can
 *    decline down the line to the one carrying the biggest damage number and take *that* one. Once
 *    mirrored, the ability stops triggering for the turn and any instance still on the stack does
 *    nothing as it resolves. The trigger cap would be spent by the first trigger — even declined —
 *    and make the rest unreachable. Declining costs nothing: the engine lowers the flag into
 *    [com.wingedsheep.sdk.scripting.effects.Gate.OnceEachTurn] gates around the [MayEffect] consent
 *    gate, so only an action actually taken spends the turn's single use, and the "you may" is asked
 *    as each instance resolves (the Legolas, Counter of Kills ruling) rather than all at once.
 *
 *  - **The prompts carry their damage number.** Declining down to the biggest hit is only a real
 *    choice if the player can tell the instances apart, and the printed sentence ("that much
 *    damage") is identical on all of them. A [DynamicHint] over the same
 *    [ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT] the effect uses renders the resolving instance's
 *    number under the prompt, so a 1/2/5 multi-block reads as three distinct questions.
 */

private val JenniferWaltersFront = card("Jennifer Walters") {
    manaCost = "{1}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Advisor Hero"
    power = 2
    toughness = 3
    oracleText = "Your opponents can't cast spells during your turn.\n" +
        "{3}{G}{W}{W}: Transform Jennifer Walters. Activate only as a sorcery."

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{W}{W}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Jennifer Walters. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Taurin Clarke"
        flavorText = "\"I will remind the witness that I have permission to treat her as hostile.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61237530-ad49-469c-a952-67c92315708e.jpg?1783902978"
    }
}

private val TheSensationalSheHulkBack = card("The Sensational She-Hulk") {
    manaCost = "{3}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Gamma Hero"
    power = 6
    toughness = 6
    oracleText = "Reach, trample\n" +
        "Your opponents can't cast spells during your turn.\n" +
        "Whenever a creature you control is dealt damage, you may have The Sensational She-Hulk " +
        "deal that much damage to any target. Do this only once each turn."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    staticAbility {
        ability = PlayersCantCastSpells(affected = Player.EachOpponent, condition = IsYourTurn)
    }

    triggeredAbility {
        trigger = TriggerSpec(
            DealsDamageEvent(recipient = RecipientFilter.CreatureYouControl),
            TriggerBinding.ANY,
        )
        val victim = target("any target", Targets.Any)
        effect = MayEffect(
            Effects.DealDamage(
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
                victim,
            ),
            // Without this the three prompts of a multi-block are the same sentence three times
            // and the player picks blind — see the KDoc's note on choosing the biggest number.
            dynamicHint = DynamicHint(
                "This trigger would deal {n} damage.",
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            ),
        )
        effectOncePerTurn = true
        description = "Whenever a creature you control is dealt damage, you may have The " +
            "Sensational She-Hulk deal that much damage to any target. Do this only once each turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "18"
        artist = "Taurin Clarke"
        imageUri = "https://cards.scryfall.io/normal/back/6/1/61237530-ad49-469c-a952-67c92315708e.jpg?1783902978"
    }
}

val JenniferWalters: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = JenniferWaltersFront,
    backFace = TheSensationalSheHulkBack,
)
