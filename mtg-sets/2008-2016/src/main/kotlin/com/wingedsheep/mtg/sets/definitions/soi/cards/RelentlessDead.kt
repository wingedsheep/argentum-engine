package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relentless Dead
 * {B}{B}
 * Creature — Zombie
 * 2/2
 * Menace
 * When this creature dies, you may pay {B}. If you do, return it to its owner's hand.
 * When this creature dies, you may pay {X}. If you do, return another target Zombie creature card
 * with mana value X from your graveyard to the battlefield.
 *
 * The second trigger's X is not in a cost, and its target restriction refers to it, so the
 * controller fixes X as the ability goes on the stack (CR 107.3f) — choosing the target *is*
 * choosing X. The trigger therefore targets any other Zombie creature card in your graveyard, and
 * on resolution offers to pay that card's mana value. Payment is a plain [Effects.PayDynamicMana]
 * gate (not the fixed-cost optional-mana shape), so the target is chosen when the trigger is put
 * on the stack and the payment is asked as it resolves, as the 2016 ruling requires. If the
 * target has left the graveyard, the ability doesn't resolve and nothing is paid.
 */
val RelentlessDead = card("Relentless Dead") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature dies, you may pay {B}. If you do, return it to its owner's hand.\n" +
        "When this creature dies, you may pay {X}. If you do, return another target Zombie creature card with mana value X from your graveyard to the battlefield."
    power = 2
    toughness = 2

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.MayPay(
            ManaCost.parse("{B}"),
            Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val zombie = target(
            TargetFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).ownedByYou(),
                zone = Zone.GRAVEYARD,
                excludeSelf = true
            )
        )
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(DynamicAmounts.targetManaValue(0)),
            then = Effects.PutOntoBattlefieldFromGraveyard(zombie)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "131"
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a0d7998-2f3d-43b8-a0be-6ff7e5c83223.jpg?1783937766"
        ruling(
            "2016-04-08",
            "Relentless Dead's last two abilities trigger at the same time and can be put on the stack in either order. The ability put on the stack first will be the last to resolve."
        )
        ruling(
            "2016-04-08",
            "You decide whether to pay for each triggered ability as it resolves. If you do, the rest of that ability's effect happens before any player may take actions."
        )
    }
}
