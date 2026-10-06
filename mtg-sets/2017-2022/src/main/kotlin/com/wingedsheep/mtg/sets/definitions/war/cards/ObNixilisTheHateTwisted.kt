package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ob Nixilis, the Hate-Twisted
 * {3}{B}{B}
 * Legendary Planeswalker — Nixilis
 * Starting Loyalty: 5
 *
 * Whenever an opponent draws a card, Ob Nixilis deals 1 damage to that player.
 * −2: Destroy target creature. Its controller draws two cards.
 *
 * `Triggers.anOpponent.draws()` fires once per card drawn, so the −2's two draws by an opponent
 * trigger the first ability twice (while Ob Nixilis is still on the battlefield). The draw follows
 * the destroy and reads the creature's last-known controller, so an indestructible creature's
 * controller still draws.
 */
val ObNixilisTheHateTwisted = card("Ob Nixilis, the Hate-Twisted") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Nixilis"
    startingLoyalty = 5
    oracleText = "Whenever an opponent draws a card, Ob Nixilis deals 1 damage to that player.\n" +
        "−2: Destroy target creature. Its controller draws two cards."

    triggeredAbility {
        trigger = Triggers.anOpponent.draws()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then
            Effects.DrawCards(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14916e2d-73af-4747-a927-d2d4cb3e32db.jpg?1783933441"

        ruling("2019-05-03", "If the target creature is an illegal target by the time Ob Nixilis's last ability tries to resolve, the ability doesn't resolve. No player draws two cards. If the target is legal but not destroyed (most likely because it has indestructible), its controller does draw two cards.")
        ruling("2019-05-03", "If Ob Nixilis leaves the battlefield before his last ability resolves, most likely because he only had 2 loyalty when you activated the ability, his first ability won't exist to trigger when the creature's controller draws two cards.")
    }
}
