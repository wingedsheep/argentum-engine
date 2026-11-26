package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska the Unseen — Return to Ravnica #208
 * {3}{B}{G} · Legendary Planeswalker — Vraska · Loyalty 5
 *
 * - +1 is a delayed trigger lasting until your next turn
 *   ([DelayedTriggerExpiry.UntilControllersNextTurn]) scoped by *recipient* to Vraska herself
 *   (`watchedRecipient = Self`, Glyph of Life's shape): each creature that deals combat damage to
 *   her triggers it separately, and "that creature" is the damage source. It still triggers when
 *   that damage kills Vraska — the trigger isn't hers, it was created by her ability.
 * - −3 is a plain targeted destroy of a nonland permanent.
 * - −7 makes three 1/1 black Assassins, each carrying its own self combat-damage trigger whose
 *   "that player" is the damaged player — including you, if an Assassin ever hits you (ruling).
 */
val VraskaTheUnseen = card("Vraska the Unseen") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 5
    oracleText = "+1: Until your next turn, whenever a creature deals combat damage to Vraska, destroy " +
        "that creature.\n" +
        "−3: Destroy target nonland permanent.\n" +
        "−7: Create three 1/1 black Assassin creature tokens with \"Whenever this token deals combat " +
        "damage to a player, that player loses the game.\""

    loyaltyAbility(+1) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(),
            watchedRecipient = EffectTarget.Self,
            effect = Effects.Destroy(EffectTarget.DamageSource),
            expiry = DelayedTriggerExpiry.UntilControllersNextTurn,
        )
        description = "Until your next turn, whenever a creature deals combat damage to Vraska, destroy " +
            "that creature."
    }

    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Destroy(permanent)
        description = "Destroy target nonland permanent."
    }

    loyaltyAbility(-7) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Assassin"),
            count = 3,
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                    effect = Effects.LoseGame(EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/8/9/89eb9f92-d189-4438-b6fe-cb253055d63e.jpg?1783940382",
        )
        description = "Create three 1/1 black Assassin creature tokens with \"Whenever this token deals " +
            "combat damage to a player, that player loses the game.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "208"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8971938c-cd26-4b83-96d7-1408cd0b0de6.jpg?1783940329"
        ruling(
            "2012-10-01",
            "If an effect creates a copy of one of the Assassin creature tokens, the copy will also have " +
                "the triggered ability."
        )
        ruling(
            "2012-10-01",
            "Each Assassin token's triggered ability will trigger whenever it deals combat damage to any " +
                "player, including you."
        )
    }
}
