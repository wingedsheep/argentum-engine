package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathless Knight — Throne of Eldraine #208 (canonical printing)
 * {B/G}{B/G}{B/G}{B/G}
 * Creature — Skeleton Knight
 * 4/2
 *
 * Haste
 * When you gain life for the first time each turn, return this card from your graveyard to your hand.
 *
 * The return functions only from the graveyard, so the trigger is zone-scoped with
 * `triggerZone = Zone.GRAVEYARD` (Squee, Goblin Nabob's shape). "First time each turn" is the
 * per-player `firstThisTurn` flag on the life-gain event, so a gain earlier in the turn — before
 * the Knight reached the graveyard — means a later gain that turn can't trigger it (ruling).
 */
val DeathlessKnight = card("Deathless Knight") {
    manaCost = "{B/G}{B/G}{B/G}{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Skeleton Knight"
    oracleText = "Haste\nWhen you gain life for the first time each turn, return this card from your graveyard to your hand."
    power = 4
    toughness = 2

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.you.gainsLife(true)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Igor Kieryluk"
        flavorText = "\"Headed to the wilds? Beware the dead riders who serve the Shadow Queen.\"\n" +
            "—Scalan, Edgewall innkeeper"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39c6e952-97b5-435c-a7fd-6271ddc23200.jpg?1783932590"
        ruling(
            "2019-10-04",
            "Deathless Knight must be in your graveyard immediately after you gain life for its last ability " +
                "to trigger. If you gain life due to damage from a source with lifelink, and that damage is dealt " +
                "at the same time that lethal damage is dealt to Deathless Knight, it won't be in your graveyard " +
                "until state-based actions are performed a moment later, and so the ability can't trigger."
        )
        ruling(
            "2019-10-04",
            "If Deathless Knight enters your graveyard after you've gained life in a turn, its last ability " +
                "can't trigger during that turn."
        )
    }
}
