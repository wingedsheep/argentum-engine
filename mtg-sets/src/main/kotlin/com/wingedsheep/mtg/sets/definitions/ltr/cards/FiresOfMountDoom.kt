package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fires of Mount Doom
 * {2}{R}
 * Legendary Enchantment
 *
 * When Fires of Mount Doom enters, it deals 2 damage to target creature an opponent
 * controls. Destroy all Equipment attached to that creature.
 * {2}{R}: Exile the top card of your library. You may play that card this turn. When you
 * play a card this way, Fires of Mount Doom deals 2 damage to each player.
 *
 * The activated ability is the standard impulse pipeline (Gather → Exile → GrantMayPlay),
 * with the rider expressed via [GrantMayPlayFromExile]'s `onPlayRider`: playing the exiled
 * card fires "Fires of Mount Doom deals 2 damage to each player" on the stack.
 */
val FiresOfMountDoom = card("Fires of Mount Doom") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment"
    oracleText = "When Fires of Mount Doom enters, it deals 2 damage to target creature an " +
        "opponent controls. Destroy all Equipment attached to that creature.\n" +
        "{2}{R}: Exile the top card of your library. You may play that card this turn. When " +
        "you play a card this way, Fires of Mount Doom deals 2 damage to each player."

    // ETB: 2 damage to target creature an opponent controls, then destroy all Equipment on it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature", Targets.CreatureOpponentControls)
        effect = Effects.Composite(
            listOf(
                Effects.DealDamage(2, creature, damageSource = EffectTarget.Self),
                Effects.DestroyAllEquipmentOnTarget(creature),
            )
        )
    }

    // {2}{R}: impulse-exile the top card; play it this turn; when played, 2 damage to each player.
    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "exiledCard"
                ),
                MoveCollectionEffect(
                    from = "exiledCard",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                Effects.GrantMayPlayFromExile(
                    from = "exiledCard",
                    onPlayRider = Effects.DealDamage(
                        2,
                        EffectTarget.PlayerRef(Player.Each),
                        damageSource = EffectTarget.Self
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "294"
        artist = "Shahab Alizadeh"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9db8702-fc72-453b-afc8-62266f7cd1c2.jpg?1687424796"
    }
}
