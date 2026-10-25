package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hive of the Eye Tyrant
 * Land
 * If you control two or more other lands, this land enters tapped.
 * {T}: Add {B}.
 * {3}{B}: Until end of turn, this land becomes a 3/3 black Beholder creature with menace and
 * "Whenever this creature attacks, exile target card from defending player's graveyard."
 * It's still a land.
 *
 * The quoted ability is granted until end of turn with [Effects.GrantTriggeredAbility] (the Raging
 * Ravine shape). "Defending player's graveyard" is `ownedBy(PlayerRef(DefendingPlayer))`, which the
 * predicate evaluator resolves at target-selection time off the attacking source, so in multiplayer
 * only the player this land is attacking qualifies.
 */
val HiveOfTheEyeTyrant = card("Hive of the Eye Tyrant") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "If you control two or more other lands, this land enters tapped.\n{T}: Add {B}.\n" +
        "{3}{B}: Until end of turn, this land becomes a 3/3 black Beholder creature with menace and " +
        "\"Whenever this creature attacks, exile target card from defending player's graveyard.\" It's still a land."

    replacementEffect(
        EntersTapped(unlessCondition = Conditions.Not(Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.MENACE),
            creatureTypes = setOf("Beholder"),
            colors = setOf("BLACK"),
            duration = Duration.EndOfTurn,
        ) then Effects.GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                val card = target(TargetFilter.CardInGraveyard.ownedBy(EffectTarget.PlayerRef(Player.DefendingPlayer)))
                effect = Effects.Exile(card)
            },
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
        description = "{3}{B}: Until end of turn, this land becomes a 3/3 black Beholder creature with menace and " +
            "\"Whenever this creature attacks, exile target card from defending player's graveyard.\" It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9eb391dc-0378-4793-a5de-899b09792a4b.jpg?1783926433"
    }
}
