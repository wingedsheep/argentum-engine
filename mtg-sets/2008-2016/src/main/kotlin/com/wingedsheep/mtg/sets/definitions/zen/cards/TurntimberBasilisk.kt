package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Turntimber Basilisk
 * {1}{G}{G}
 * Creature — Basilisk
 * 2/1
 * Deathtouch
 * Landfall — Whenever a land you control enters, you may have target creature block this creature
 * this turn if able.
 *
 * Landfall is the Grazing Gladehart trigger; the "you may" is decided on resolution (per the 2009
 * ruling), which is the builder's `optional = true` gate around [Effects.ForceBlock] — the same
 * forced-block effect Rampant Elephant and Matsu-Tribe Decoy activate.
 */
val TurntimberBasilisk = card("Turntimber Basilisk") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Basilisk"
    power = 2
    toughness = 1
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\nLandfall — Whenever a land you control enters, you may have target creature block this creature this turn if able."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.Creature)
        optional = true
        effect = Effects.ForceBlock(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Goran Josic"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b774563a-d2d4-4bdf-ad82-dd9e1fa953ba.jpg?1783942129"
        ruling("2009-10-01", "Tapped creatures, creatures that can't block as the result of an effect, creatures with unpaid costs to block (such as those from War Cadence), and creatures that aren't controlled by the defending player are exempt from effects that would require them to block. Such creatures can be targeted by Turntimber Basilisk's landfall ability, but the requirement to block does nothing.")
        ruling("2009-10-01", "You decide whether to have the targeted creature block Turntimber Basilisk this turn if able at the time the landfall ability resolves, not at the time Turntimber Basilisk attacks.")
        ruling("2009-10-01", "If Turntimber Basilisk's landfall ability triggers multiple times during the same turn, you can have multiple creatures block it that turn if able.")
        ruling("2009-10-01", "Deciding to have the targeted creature block doesn't force you to attack with Turntimber Basilisk that turn. If Turntimber Basilisk doesn't attack, the targeted creature is free to block whichever creature its controller chooses, or block no creatures at all.")
    }
}
