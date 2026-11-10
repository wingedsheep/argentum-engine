package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eldrazi Displacer
 * {2}{W}
 * Creature — Eldrazi
 * 3/3
 * Devoid (This card has no color.)
 * {2}{C}: Exile another target creature, then return it to the battlefield tapped under its
 * owner's control. ({C} represents colorless mana.)
 *
 * The blink is The Mighty Thor, Jane Foster's pair — [Effects.Move] to [Zone.EXILE] then back to
 * [Zone.BATTLEFIELD] with [ZonePlacement.Tapped]. No controller override: an effect that puts a
 * permanent onto the battlefield without naming a controller puts it there under its owner's
 * control, which is the printed wording. A token exiled this way stays in exile until it ceases to
 * exist, so the return step does nothing for it (the engine refuses to move a token out of a
 * non-battlefield zone).
 */
val EldraziDisplacer = card("Eldrazi Displacer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{2}{C}: Exile another target creature, then return it to the battlefield tapped under its " +
        "owner's control. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{2}{C}")
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.Move(creature, Zone.EXILE) then
            Effects.Move(creature, Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0bb1a5c-0f59-4951-827f-fe9df968232d.jpg?1783937928"
        ruling(
            "2016-01-22",
            "If a creature token is exiled this way, it will cease to exist and will not return to the battlefield."
        )
        ruling(
            "2016-01-22",
            "After the creature returns to the battlefield, it will be a new object with no connection to the " +
                "creature that was exiled. It won't be in combat or have any additional abilities it may have had " +
                "when it was exiled. Any +1/+1 counters on it or Auras attached to it are removed, and any " +
                "Equipment will no longer be attached."
        )
    }
}
