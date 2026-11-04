package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska, Scheming Gorgon
 * {4}{B}{B}
 * Legendary Planeswalker — Vraska
 * Starting Loyalty: 5
 *
 * +2: Creatures you control get +1/+0 until end of turn.
 * −3: Destroy target creature.
 * −10: Until end of turn, creatures you control gain deathtouch and "Whenever this creature deals
 * damage to an opponent, that player loses the game."
 *
 * The +2 and −10 lock their group in at resolution (CR 611.2c): creatures you gain control of later
 * in the turn get neither the bonus nor the abilities (ruling). The −10 is the Storm the Citadel
 * shape — `ForEachInGroup` over your creatures, granting deathtouch and a triggered ability to each
 * until end of turn. The granted trigger is `Triggers.self.dealsDamage(Recipient.Opponent)`, any
 * damage type (combat or not, per the ruling), and "that player" is the damaged player
 * ([Player.TriggeringPlayer]), as on Phage the Untouchable.
 */
val VraskaSchemingGorgon = card("Vraska, Scheming Gorgon") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 5
    oracleText = "+2: Creatures you control get +1/+0 until end of turn.\n" +
        "−3: Destroy target creature.\n" +
        "−10: Until end of turn, creatures you control gain deathtouch and \"Whenever this creature " +
        "deals damage to an opponent, that player loses the game.\""

    val damagedOpponentLoses = grantedTriggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.LoseGame(
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            message = "Dealt damage by a creature empowered by Vraska, Scheming Gorgon",
        )
        description = "Whenever this creature deals damage to an opponent, that player loses the game."
    }

    // +2: Creatures you control get +1/+0 until end of turn.
    loyaltyAbility(+2) {
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl)
    }

    // −3: Destroy target creature.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    // −10: Until end of turn, creatures you control gain deathtouch and "Whenever this creature deals
    // damage to an opponent, that player loses the game."
    loyaltyAbility(-10) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.IterationEntity) then
                Effects.GrantTriggeredAbility(damagedOpponentLoses, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        inBooster = false
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91685732-c118-4f52-ac77-f96866a687a3.jpg?1783935259"
        ruling(
            "2018-01-19",
            "Vraska's first and last abilities affect only creatures you control at the time it resolves. " +
                "Creatures you begin to control later in the turn won't get +1/+0 or gain abilities.",
        )
        ruling(
            "2018-01-19",
            "The triggered ability Vraska grants to creatures with her third ability triggers on any damage " +
                "those creatures deal, including noncombat damage. If multiple players are dealt damage " +
                "simultaneously by affected creatures, you choose the order in which those triggers resolve. " +
                "If all your opponents have lost the game, you'll win the game before any more of the " +
                "triggers resolve.",
        )
    }
}
