package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kami of Restless Shadows — Kamigawa: Neon Dynasty #104 (canonical printing)
 * {4}{B} · Creature — Spirit · 3/3
 *
 * When this creature enters, choose one —
 * • Return up to one target Ninja or Rogue creature card from your graveyard to your hand.
 * • Put target creature card from your graveyard on top of your library.
 *
 * The first mode's target is optional ("up to one"), so it can always be chosen; the second mode
 * needs a creature card in your graveyard to be chosen at all.
 */
val KamiOfRestlessShadows = card("Kami of Restless Shadows") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, choose one —\n" +
        "• Return up to one target Ninja or Rogue creature card from your graveyard to your hand.\n" +
        "• Put target creature card from your graveyard on top of your library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = ModalEffect.chooseOne(
            mode("Return up to one target Ninja or Rogue creature card from your graveyard to your hand.") {
                val ninjaOrRogue = target(
                    TargetFilter(
                        GameObjectFilter.Creature.withAnySubtype("Ninja", "Rogue").ownedByYou(),
                        zone = Zone.GRAVEYARD,
                    ),
                    optional = true,
                )
                effect = Effects.ReturnToHand(ninjaOrRogue)
            },
            mode("Put target creature card from your graveyard on top of your library.") {
                val creature = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.PutOnTopOfLibrary(creature)
            },
        )
        description = "When this creature enters, choose one — " +
            "• Return up to one target Ninja or Rogue creature card from your graveyard to your hand. " +
            "• Put target creature card from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62d3be4a-ab4f-46ef-837c-0f7559a701fe.jpg?1783923883"
    }
}
