package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Torrent Elemental — Fate Reforged #56 (canonical printing)
 * {4}{U} · Creature — Elemental · 3/5
 *
 * Flying
 * Whenever this creature attacks, tap all creatures defending player controls.
 * {3}{B/G}{B/G}: Put this card from exile onto the battlefield tapped. Activate only as a sorcery.
 *
 * The last ability functions only while the card is in exile (`activateFromZone = Zone.EXILE`),
 * and only face up: a card exiled face down has no characteristics (CR 406.3a), so the engine
 * doesn't offer its printed abilities there. The move is guarded on the card still being in exile.
 */
val TorrentElemental = card("Torrent Elemental") {
    manaCost = "{4}{U}"
    colorIdentity = "BGU"
    typeLine = "Creature — Elemental"
    oracleText = "Flying\n" +
        "Whenever this creature attacks, tap all creatures defending player controls.\n" +
        "{3}{B/G}{B/G}: Put this card from exile onto the battlefield tapped. Activate only as a sorcery."
    power = 3
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.tapAll(
            GroupFilter(
                GameObjectFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{B/G}{B/G}")
        effect = Effects.PutOntoBattlefield(EffectTarget.Self, tapped = true, fromZone = Zone.EXILE)
        activateFromZone = Zone.EXILE
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "56"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc4850e4-acb9-458d-952f-b3952cab2a5b.jpg?1783938700"
    }
}
