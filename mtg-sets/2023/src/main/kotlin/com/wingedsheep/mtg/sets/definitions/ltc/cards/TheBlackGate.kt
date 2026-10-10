package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * The Black Gate
 * Legendary Land — Gate
 *
 * As The Black Gate enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {B}.
 * {1}{B}, {T}: Choose a player with the most life or tied for most life. Target creature can't
 * be blocked by creatures that player controls this turn.
 *
 * The creature is targeted on activation; the player is chosen (not targeted) on resolution. The
 * grant freezes *that player* while the set of their creatures stays live, as the rulings require.
 */
val TheBlackGate = card("The Black Gate") {
    typeLine = "Legendary Land — Gate"
    colorIdentity = "B"
    oracleText = "As The Black Gate enters, you may pay 3 life. If you don't, it enters tapped.\n" +
        "{T}: Add {B}.\n" +
        "{1}{B}, {T}: Choose a player with the most life or tied for most life. Target creature " +
        "can't be blocked by creatures that player controls this turn."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val player = selectTarget(
                TargetPlayer(
                    restriction = Conditions.candidateHasMostLife(),
                    descriptionOverride = "a player with the most life or tied for most life"
                ),
                nonTargeting = true
            )
            run(
                Effects.GrantStaticAbility(
                    CantBeBlockedBy(
                        GameObjectFilter.Creature.withControllerPredicate(
                            ControllerPredicate.ControlledByReferencedPlayer(player.asTarget)
                        )
                    ),
                    creature,
                    Duration.EndOfTurn
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Marc Simonetti"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46418186-c215-47c4-9d0a-d15a1d8ca613.jpg?1783916010"
        ruling("2023-06-16", "The chosen player needs only to have the most life or tied for most life as The Black Gate's last ability resolves. After the ability resolves, any changes in life total won't affect which creatures can block or be blocked that turn.")
        ruling("2023-06-16", "The creature that was targeted by The Black Gate's last ability can't be blocked by any creature the chosen player controls, including creatures that weren't on the battlefield when The Black Gate's ability resolved.")
    }
}
