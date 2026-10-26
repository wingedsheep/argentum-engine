package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nether Traitor
 * {B}{B}
 * Creature — Spirit
 * 1/1
 *
 * Haste
 * Shadow
 * Whenever another creature is put into your graveyard from the battlefield, you may pay {B}.
 * If you do, return this card from your graveyard to the battlefield.
 *
 * The recursion functions only from the graveyard (`triggerZone = GRAVEYARD`). "Put into your
 * graveyard" is ownership, not control — a creature you own dying under an opponent's control
 * still lands in your graveyard — so the subject is `Creature.ownedByYou()`. Once the Traitor has
 * returned, further pending instances find it gone from the graveyard and do nothing even if paid
 * for (ruling 2021-03-19).
 */
val NetherTraitor = card("Nether Traitor") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Shadow (This creature can block or be blocked by only creatures with shadow.)\n" +
        "Whenever another creature is put into your graveyard from the battlefield, you may pay {B}. " +
        "If you do, return this card from your graveyard to the battlefield."

    keywords(Keyword.HASTE, Keyword.SHADOW)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.ownedByYou()).dies()
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{B}"),
            then = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD),
        )
        description = "Whenever another creature is put into your graveyard from the battlefield, you may pay " +
            "{B}. If you do, return Nether Traitor from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Vance Kovacs"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/911d2271-8998-4193-9d1a-1768c5dfaaad.jpg?1783943230"
        ruling(
            "2021-03-19",
            "If multiple creatures are put into your graveyard at the same time, Nether Traitor's ability triggers " +
                "for each of them. Once you return it to the battlefield, you may pay {B} for the other abilities as " +
                "they resolve, but they'll have no effect if you do. Even if Nether Traitor returns to your " +
                "graveyard, it's considered a new object and won't be returned."
        )
        ruling(
            "2021-03-19",
            "If Nether Traitor and another creature are put into your graveyard at the same time, Nether Traitor's " +
                "ability won't trigger. This is because it must be in your graveyard before the creature dies in " +
                "order for its ability that returns it to the battlefield to trigger."
        )
    }
}
