package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathrite Shaman — Return to Ravnica #213 (canonical printing)
 * {B/G} · Creature — Elf Shaman · 1/2
 *
 * {T}: Exile target land card from a graveyard. Add one mana of any color.
 * (Activate only as an instant.)
 * {B}, {T}: Exile target instant or sorcery card from a graveyard. Each opponent loses 2 life.
 * {G}, {T}: Exile target creature card from a graveyard. You gain 2 life.
 *
 * The first ability targets, so it is NOT a mana ability (CR 605.1a): it is a plain
 * `activatedAbility`, uses the stack and can be responded to — the same shape as Witch Engine.
 * Each ability's effects all hang off its single target, so an illegal target on resolution
 * fizzles the whole ability (no mana, no life change).
 */
val DeathriteShaman = card("Deathrite Shaman") {
    manaCost = "{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Shaman"
    power = 1
    toughness = 2
    oracleText = "{T}: Exile target land card from a graveyard. Add one mana of any color. " +
        "(Activate only as an instant.)\n" +
        "{B}, {T}: Exile target instant or sorcery card from a graveyard. Each opponent loses 2 life.\n" +
        "{G}, {T}: Exile target creature card from a graveyard. You gain 2 life."

    activatedAbility {
        cost = Costs.Tap
        val land = target(TargetFilter(GameObjectFilter.Land, zone = Zone.GRAVEYARD))
        effect = Effects.Exile(land) then Effects.AddAnyColorMana()
        description = "{T}: Exile target land card from a graveyard. Add one mana of any color."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val spellCard = target(TargetFilter.InstantOrSorceryInGraveyard)
        effect = Effects.Exile(spellCard) then
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "{B}, {T}: Exile target instant or sorcery card from a graveyard. Each opponent loses 2 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Exile(creatureCard) then Effects.GainLife(2)
        description = "{G}, {T}: Exile target creature card from a graveyard. You gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70496f16-c4c0-4c03-beef-454eb4824cd1.jpg?1783940328"
        ruling("2016-06-08", "Because the first ability requires a target, it is not a mana ability. It uses the stack and can be responded to.")
        ruling("2016-06-08", "If the target of any of Deathrite Shaman's three abilities is an illegal target when that ability tries to resolve, it won't resolve and none of its effects will happen. You won't add mana, no opponent will lose life, or you won't gain life, as appropriate.")
    }
}
