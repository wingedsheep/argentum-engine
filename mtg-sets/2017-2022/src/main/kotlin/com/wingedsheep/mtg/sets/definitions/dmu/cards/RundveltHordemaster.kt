package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rundvelt Hordemaster — Dominaria United #142
 * {1}{R} · Creature — Goblin Warrior · 1/1
 *
 * Other Goblins you control get +1/+1.
 * Whenever this creature or another Goblin you control dies, exile the top card of your library.
 * If it's a Goblin creature card, you may cast that card until the end of your next turn.
 *
 * "This creature or another Goblin" is two triggers sharing one effect: a self binding, so
 * Hordemaster's own death fires even if it stopped being a Goblin, and an ANOTHER binding over Goblins
 * you control — each Goblin dying simultaneously triggers it separately (ruling).
 * "If it's a Goblin creature card" tests the exiled *card*, so the exiled collection is filtered
 * before the grant (the Chandra, Dressed to Kill shape); a non-Goblin card simply stays exiled.
 * "Cast" never covers playing a land, hence `nonLandOnly`.
 */
val RundveltHordemaster = card("Rundvelt Hordemaster") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Other Goblins you control get +1/+1.\n" +
        "Whenever this creature or another Goblin you control dies, exile the top card of your " +
        "library. If it's a Goblin creature card, you may cast that card until the end of your next turn."

    val hordeEffect = Effects.Pipeline {
        val hordeExiled = gather(CardSource.TopOfLibrary(1))
        exile(hordeExiled)
        val hordeGoblin = filter(
            hordeExiled,
            GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
        )
        run(
            Effects.GrantMayPlayFromExile(
                hordeGoblin,
                expiry = MayPlayExpiry.UntilEndOfNextTurn,
                nonLandOnly = true
            )
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN).youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = hordeEffect
        description = "Whenever this creature or another Goblin you control dies, exile the top " +
            "card of your library. If it's a Goblin creature card, you may cast that card until " +
            "the end of your next turn."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN).youControl()).dies()
        effect = hordeEffect
        description = "Whenever this creature or another Goblin you control dies, exile the top " +
            "card of your library. If it's a Goblin creature card, you may cast that card until " +
            "the end of your next turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Bruno Biazotto"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffee9dbe-af90-4e19-844d-4df1180f24c4.jpg?1783921311"
        ruling(
            "2022-09-09",
            "If Rundvelt Hordemaster and one or more other Goblins you control die at the same " +
                "time, its ability will trigger once for each of them."
        )
    }
}
