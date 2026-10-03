package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Molten Gatekeeper — Modern Horizons 3 #128 (common)
 * {2}{R} · Artifact Creature — Golem · 2/3
 *
 * Whenever another creature you control enters, this creature deals 1 damage to each opponent.
 * Unearth {R} ({R}: Return this card from your graveyard to the battlefield. It gains haste. Exile it
 * at the beginning of the next end step or if it would leave the battlefield. Unearth only as a
 * sorcery.)
 *
 * Unearth (CR 702.84a) has no keyword of its own in the SDK, so it is spelled out as the activated
 * ability its reminder text describes: a sorcery-speed graveyard activation that returns this card,
 * grants haste, grants the exile-instead-of-leaving replacement ([Effects.GrantExileOnLeave], as on
 * Kheru Lich Lord), and schedules a next-end-step exile. The return is gated on the card still being
 * in the graveyard at resolution — if it left in response, the ability does nothing.
 */
val MoltenGatekeeper = card("Molten Gatekeeper") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Golem"
    power = 2
    toughness = 3
    oracleText = "Whenever another creature you control enters, this creature deals 1 damage to each opponent.\n" +
        "Unearth {R} ({R}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. " +
        "Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.If(
            Conditions.SourceInZone(Zone.GRAVEYARD),
            Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.Permanent) then
                Effects.GrantExileOnLeave(EffectTarget.Self) then
                Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.BATTLEFIELD)
                )
        )
        description = "Unearth {R}: Return this card from your graveyard to the battlefield. It gains haste. " +
            "Exile it at the beginning of the next end step or if it would leave the battlefield. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f5ba065-2806-4e99-a330-168cfe76250f.jpg?1783911269"
        ruling("2024-06-07", "If you activate a card's unearth ability but that card is removed from your graveyard before the ability resolves, that unearth ability will do nothing as it resolves.")
        ruling("2024-06-07", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead.")
    }
}
