package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.core.Keyword

/**
 * Tomik, Wielder of Law — Murders at Karlov Manor #431
 * {1}{W}{B} · Legendary Creature — Human Advisor · 2/4
 *
 * Affinity for planeswalkers (This spell costs {1} less to cast for each planeswalker you control.)
 * Flying, vigilance
 * Whenever an opponent attacks with creatures, if two or more of those creatures are attacking you
 * and/or planeswalkers you control, that opponent loses 3 life and you draw a card.
 *
 * **Affinity needed no new engine vocabulary.** [KeywordAbility.Affinity] is parameterized by
 * [CardType] and `CostCalculator.countPermanentsOfType` counts off the *projected* battlefield, so
 * `Affinity(CardType.PLANESWALKER)` reduces correctly — including for a creature-land animated into
 * a planeswalker, or a planeswalker whose control changed. Only Assay's keyword grammar had to
 * learn the spelling.
 *
 * **The trigger is the defender-side batch trigger widened to planeswalkers.**
 * [EventPattern.CreaturesAttackYouEvent] deliberately implements CR 509.1b the narrow way — an
 * attacker aimed at *your planeswalker* does not count as attacking *you* — because that is what
 * Orim's Prayer needs. Tomik prints the wider reading explicitly ("you and/or planeswalkers you
 * control"), so it opts in via `includePlaneswalkersYouControl`. Battles stay out: a battle you
 * protect is controlled by its caster, not by you.
 *
 * **`minAttackers = 2` and the [interveningIf] are both required, and they are not redundant.**
 * The printed "if two or more…" is an intervening-if clause, so CR 603.4 checks it *twice*: once
 * when the ability would trigger, and again as it resolves. `minAttackers = 2` is the first check
 * (an attack by a single creature never puts the ability on the stack at all); the condition is the
 * second (an attacker removed from combat in response makes the ability do nothing). Modelling only
 * the first would be the classic "looks right, resolves wrong" approximation.
 *
 * "That opponent" is [Player.TriggeringPlayer] — the attacking player, which
 * `AttackersDeclaredEvent` now publishes into the trigger context. [Player.AnOpponent] would be
 * wrong at a multiplayer table, where the attacker need not be the first opponent in turn order.
 */
val TomikWielderOfLaw = card("Tomik, Wielder of Law") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    oracleText = "Affinity for planeswalkers (This spell costs {1} less to cast for each " +
        "planeswalker you control.)\n" +
        "Flying, vigilance\n" +
        "Whenever an opponent attacks with creatures, if two or more of those creatures are " +
        "attacking you and/or planeswalkers you control, that opponent loses 3 life and you draw " +
        "a card."
    power = 2
    toughness = 4

    keywordAbility(KeywordAbility.Affinity(CardType.PLANESWALKER))
    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.CreaturesAttackYouEvent(
                minAttackers = 2,
                includePlaneswalkersYouControl = true,
            ),
            binding = TriggerBinding.ANY,
        )
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(
                Player.Each,
                GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers(),
            ).count(),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2),
        )
        effect = Effects.Composite(
            Effects.LoseLife(3, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            Effects.DrawCards(1),
        )
        description = "Whenever an opponent attacks with creatures, if two or more of those " +
            "creatures are attacking you and/or planeswalkers you control, that opponent loses 3 " +
            "life and you draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "431"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c5a7550-fe1a-4797-9583-70ab56cfac0d.jpg?1783912763"
    }
}
