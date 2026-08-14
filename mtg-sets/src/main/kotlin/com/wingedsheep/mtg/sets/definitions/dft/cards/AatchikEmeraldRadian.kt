package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aatchik, Emerald Radian
 * {3}{B}{B}{G}
 * Legendary Creature — Insect Druid
 * 3/3
 * When Aatchik enters, create a 1/1 green Insect creature token for each artifact and/or creature
 * card in your graveyard.
 * Whenever another Insect you control dies, put a +1/+1 counter on Aatchik. Each opponent loses 1
 * life.
 *
 * The token count is one `Count` over the graveyard with a single artifact-or-creature predicate,
 * so an artifact creature card is counted once rather than twice (Scryfall ruling below).
 */
val AatchikEmeraldRadian = card("Aatchik, Emerald Radian") {
    manaCost = "{3}{B}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Insect Druid"
    oracleText = "When Aatchik enters, create a 1/1 green Insect creature token for each artifact " +
        "and/or creature card in your graveyard.\n" +
        "Whenever another Insect you control dies, put a +1/+1 counter on Aatchik. Each opponent " +
        "loses 1 life."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            count = DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.CreatureOrArtifact),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/9/d/9d3d855d-93a1-4ec4-af19-e544f271ae10.jpg?1783907678"
        )
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.INSECT),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.OTHER
        )
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Loïc Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbdaa29b-85ff-4a06-b27e-fcdbdfd4a3fe.jpg?1783907862"
        ruling(
            "2025-02-07",
            "If a card in your graveyard is an artifact creature card, count it only once when " +
                "determining how many Insect tokens to create for Aatchik's first ability."
        )
        ruling(
            "2025-02-07",
            "If Aatchik is dealt lethal damage at the same time as another Insect you control, " +
                "Aatchik's last ability will still trigger, causing each opponent to lose 1 life, " +
                "but Aatchik won't get a +1/+1 counter in time to save it."
        )
    }
}
