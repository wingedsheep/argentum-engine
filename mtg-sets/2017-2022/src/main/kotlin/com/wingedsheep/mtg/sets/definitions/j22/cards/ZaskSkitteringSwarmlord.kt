package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zask, Skittering Swarmlord
 * {3}{G}{G}
 * Legendary Creature — Insect
 * 5/5
 * You may play lands and cast Insect spells from your graveyard.
 * Whenever another Insect you control dies, put it on the bottom of its owner's library, then
 * mill two cards.
 * {1}{B/G}: Target Insect gets +1/+0 and gains deathtouch until end of turn.
 *
 * The graveyard permission is two grants — lands (Crucible of Worlds' shape) and Insect spells —
 * both under normal timing and costs. The death trigger moves the card only if it is still in the
 * graveyard when the trigger resolves; the mill happens either way.
 */
val ZaskSkitteringSwarmlord = card("Zask, Skittering Swarmlord") {
    manaCost = "{3}{G}{G}"
    typeLine = "Legendary Creature — Insect"
    power = 5
    toughness = 5
    oracleText = "You may play lands and cast Insect spells from your graveyard.\n" +
        "Whenever another Insect you control dies, put it on the bottom of its owner's library, " +
        "then mill two cards. (Put the top two cards of your library into your graveyard.)\n" +
        "{1}{B/G}: Target Insect gets +1/+0 and gains deathtouch until end of turn. " +
        "({B/G} can be paid with either {B} or {G}.)"

    staticAbility {
        ability = MayPlayLandsFromGraveyard
    }

    staticAbility {
        ability = MayCastFromGraveyard(filter = GameObjectFilter.Nonland.withSubtype(Subtype.INSECT))
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withSubtype(Subtype.INSECT).youControl()).dies()
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.LIBRARY,
            placement = ZonePlacement.Bottom,
            fromZone = Zone.GRAVEYARD,
        ) then Patterns.Library.mill(2)
        description = "Whenever another Insect you control dies, put it on the bottom of its " +
            "owner's library, then mill two cards."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B/G}")
        val insect = target(TargetFilter.Permanent.withSubtype(Subtype.INSECT))
        effect = Effects.ModifyStats(1, 0, insect) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, insect)
        description = "Target Insect gets +1/+0 and gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eafc507-973a-4aec-91b9-c29d70b0e25d.jpg?1783919177"
        ruling(
            "2022-12-02",
            "You must pay all costs and follow all timing rules for cards played from your " +
                "graveyard this way. For example, you may play a land this way only while the stack " +
                "is empty during one of your own main phases, and only if you haven't played a land " +
                "yet this turn."
        )
    }
}
