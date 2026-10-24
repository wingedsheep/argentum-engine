package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lorehold Apprentice — Strixhaven: School of Mages #198 (canonical printing)
 * {R}{W} · Creature — Human Cleric · 2/2
 *
 * Magecraft — Whenever you cast or copy an instant or sorcery spell, until end of turn, Spirit
 * creatures you control gain "{T}: This creature deals 1 damage to each opponent."
 *
 * Magecraft is [Triggers.you] `.castsOrCopies(InstantOrSorcery)`; the grant is
 * [Effects.GrantActivatedAbilityToGroup] over the Spirit creatures you control as the trigger
 * resolves (CR 611.2c — a resolved ability-granting effect locks in its affected set). Inside the
 * granted ability `Self` is the Spirit, so the Spirit is the damage source. Each resolution adds
 * another instance, matching the ruling that multiple instances can stack.
 */
val LoreholdApprentice = card("Lorehold Apprentice") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Magecraft — Whenever you cast or copy an instant or sorcery spell, until end of " +
        "turn, Spirit creatures you control gain \"{T}: This creature deals 1 damage to each opponent.\""

    triggeredAbility {
        trigger = Triggers.you.castsOrCopies(GameObjectFilter.InstantOrSorcery)
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Spirit").youControl()),
            duration = Duration.EndOfTurn
        )
        description = "Magecraft — Whenever you cast or copy an instant or sorcery spell, until end " +
            "of turn, Spirit creatures you control gain \"{T}: This creature deals 1 damage to each opponent.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Manuel Castañón"
        flavorText = "Ancient footfalls echo in her head, their timeless rhythm guiding the present."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/048157c6-4626-4881-ba19-deddd13622dc.jpg?1783927307"
        ruling("2021-04-16", "Lorehold Apprentice's ability can give your Spirit creatures multiple instances of \"{T}: This creature deals 1 damage to each opponent.\" However, each Spirit can only activate one of these abilities at a time, and each ability will still only deal 1 damage.")
        ruling("2021-04-16", "If an effect creates a copy of an instant or sorcery spell, this will also cause the magecraft ability to trigger.")
        ruling("2021-04-16", "If an effect creates multiple copies of an instant or sorcery spell, magecraft abilities trigger once for each copy created by the effect.")
    }
}
